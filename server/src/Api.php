<?php
declare(strict_types=1);

namespace DoraDodo;

use PDO;

/**
 * Front controller logic. Every route is versioned: {API_BASE_PATH}/v{N}/{resource}.
 * A request without a version, or with a version this server does not serve, is a 404.
 */
final class Api
{
    /** Versions served by this build. Add "2" here only together with its own routes below. */
    private const VERSIONS = ['1'];
    private const MAX_BODY = 16384;
    private const TYPES = ['Bug', 'Idea', 'Question', 'Other'];

    private ?PDO $pdo = null;

    public function handle(string $method, string $uri): void
    {
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-Content-Type-Options: nosniff');
        try {
            $this->dispatch($method, $uri);
        } catch (ApiException $e) {
            $this->fail($e);
        } catch (\Throwable $e) {
            error_log('[doradodo] ' . $e);
            $this->fail(new ApiException(500, 'server_error', 'Something went wrong on our side.'));
        }
    }

    private function dispatch(string $method, string $uri): void
    {
        $path = parse_url($uri, PHP_URL_PATH) ?: '/';
        $base = rtrim(Env::get('API_BASE_PATH', '/api'), '/');
        if ($base !== '' && !str_starts_with($path, $base . '/')) {
            throw new ApiException(404, 'not_found', 'Unknown endpoint.');
        }
        $rest = substr($path, strlen($base));

        if (!preg_match('#^/v(\d+)/([a-z0-9/_-]+)$#', $rest, $m)) {
            throw new ApiException(404, 'version_required', 'Use a versioned path such as /v1/feedback.');
        }
        [, $version, $resource] = $m;
        if (!in_array($version, self::VERSIONS, true)) {
            throw new ApiException(404, 'unsupported_version', "API version v$version is not available.");
        }
        header('API-Version: ' . $version);

        switch ($resource) {
            case 'health':
                $this->allow($method, 'GET');
                $this->send(200, ['status' => 'ok']);
                return;
            case 'feedback':
                $this->allow($method, 'POST');
                $this->postFeedback();
                return;
            default:
                throw new ApiException(404, 'not_found', 'Unknown endpoint.');
        }
    }

    private function allow(string $method, string $allowed): void
    {
        if ($method !== $allowed) {
            throw new ApiException(405, 'method_not_allowed', 'Method not allowed.', ['Allow' => $allowed]);
        }
    }

    private function postFeedback(): void
    {
        $db = $this->db();
        $limiter = new RateLimiter($db, Env::require('RATE_LIMIT_SALT'));
        if (random_int(1, 100) === 1) {
            $limiter->purge();
        }

        // 1. Cheap barrier first, so unauthenticated floods never reach token verification.
        $limiter->hit('fb-ip', $this->clientIp(), Env::int('RATE_IP_PER_HOUR', 30), 3600);

        // 2. Authentication: signed-in accounts only.
        $user = $this->authenticate();

        // 3. Validate the body. A malformed request does not use up the user's quota.
        $in = $this->readJson();
        $row = $this->validate($in);

        // 4. Per-user quota.
        $limiter->hit('fb-user-h', $user['uid'], Env::int('RATE_USER_PER_HOUR', 5), 3600);
        $limiter->hit('fb-user-d', $user['uid'], Env::int('RATE_USER_PER_DAY', 20), 86400);

        $db->prepare(
            'INSERT INTO feedback (user_id, rating, type, message, app_version, sdk_int, locale, sent_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        )->execute([
            $user['uid'], $row['rating'], $row['type'], $row['message'],
            $row['appVersion'], $row['sdkInt'], $row['locale'], $row['sentAt'],
        ]);

        $this->send(201, ['accepted' => true, 'serverTime' => (int) (microtime(true) * 1000)]);
    }

    /** @return array{uid:string, provider:string} */
    private function authenticate(): array
    {
        $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
        if (!preg_match('/^Bearer ([A-Za-z0-9._-]+)$/', $h, $m)) {
            throw new ApiException(401, 'unauthenticated', 'Sign in to send feedback.', [
                'WWW-Authenticate' => 'Bearer',
            ]);
        }
        $verifier = new FirebaseVerifier(
            Env::require('FIREBASE_PROJECT_ID'),
            sys_get_temp_dir() . '/doradodo_firebase_certs.json'
        );
        return $verifier->verify($m[1]);
    }

    /** @return array<string,mixed> */
    private function readJson(): array
    {
        $type = strtolower($_SERVER['CONTENT_TYPE'] ?? '');
        if (!str_starts_with($type, 'application/json')) {
            throw new ApiException(415, 'unsupported_media_type', 'Send application/json.');
        }
        if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > self::MAX_BODY) {
            throw new ApiException(413, 'payload_too_large', 'Request body is too large.');
        }
        $raw = file_get_contents('php://input', false, null, 0, self::MAX_BODY + 1);
        if ($raw === false || strlen($raw) > self::MAX_BODY) {
            throw new ApiException(413, 'payload_too_large', 'Request body is too large.');
        }
        $data = json_decode($raw, true);
        if (!is_array($data) || array_is_list($data)) {
            throw new ApiException(400, 'invalid_body', 'Body must be a JSON object.');
        }
        return $data;
    }

    /**
     * Any "userId" the client sends is ignored: the identity comes from the verified token.
     * @param array<string,mixed> $in
     * @return array{rating:int,type:string,message:string,appVersion:?string,sdkInt:?int,locale:?string,sentAt:int}
     */
    private function validate(array $in): array
    {
        $rating = $in['rating'] ?? 0;
        if (!is_int($rating) || $rating < 0 || $rating > 5) {
            throw $this->bad('rating must be an integer from 0 to 5.');
        }
        $type = $in['type'] ?? null;
        if (!is_string($type) || !in_array($type, self::TYPES, true)) {
            throw $this->bad('type must be one of: ' . implode(', ', self::TYPES) . '.');
        }
        $message = $in['message'] ?? null;
        $message = is_string($message) ? trim($message) : '';
        $len = mb_strlen($message);
        if ($len < 1 || $len > 2000) {
            throw $this->bad('message must be 1 to 2000 characters.');
        }
        $appVersion = $this->optString($in['appVersion'] ?? null, 32, 'appVersion');
        $locale = $this->optString($in['locale'] ?? null, 16, 'locale');
        $sdk = $in['sdkInt'] ?? null;
        if ($sdk !== null && (!is_int($sdk) || $sdk < 0 || $sdk > 1000)) {
            throw $this->bad('sdkInt is invalid.');
        }
        $sentAt = $in['sentAt'] ?? null;
        if (!is_int($sentAt) || $sentAt < 0) {
            throw $this->bad('sentAt must be epoch milliseconds.');
        }
        return [
            'rating' => $rating, 'type' => $type, 'message' => $message,
            'appVersion' => $appVersion, 'sdkInt' => $sdk, 'locale' => $locale, 'sentAt' => $sentAt,
        ];
    }

    private function optString(mixed $v, int $max, string $name): ?string
    {
        if ($v === null) {
            return null;
        }
        if (!is_string($v) || mb_strlen($v) > $max) {
            throw $this->bad("$name is invalid.");
        }
        return $v;
    }

    private function bad(string $msg): ApiException
    {
        return new ApiException(422, 'validation_failed', $msg);
    }

    private function clientIp(): string
    {
        $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        $trusted = array_filter(array_map('trim', explode(',', Env::get('TRUSTED_PROXIES', '') ?? '')));
        if (in_array($ip, $trusted, true) && !empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            // The last entry is the one our own proxy appended; earlier ones are client-controlled.
            $chain = array_map('trim', explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']));
            $last = end($chain);
            if (filter_var($last, FILTER_VALIDATE_IP)) {
                return $last;
            }
        }
        return $ip;
    }

    private function db(): PDO
    {
        return $this->pdo ??= new PDO(
            Env::require('DB_DSN'),
            Env::get('DB_USER'),
            Env::get('DB_PASS'),
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_EMULATE_PREPARES => false,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]
        );
    }

    /** @param array<string,mixed> $body */
    private function send(int $status, array $body): void
    {
        http_response_code($status);
        echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }

    private function fail(ApiException $e): void
    {
        foreach ($e->headers as $k => $v) {
            header("$k: $v");
        }
        $this->send($e->status, ['error' => ['code' => $e->errorCode, 'message' => $e->getMessage()]]);
    }
}
