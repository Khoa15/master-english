<?php
declare(strict_types=1);

namespace DoraDodo;

/**
 * Verifies a Firebase Authentication ID token (RS256 JWT) without any library:
 * signature against Google's published certificates, then iss / aud / exp / sub.
 * https://firebase.google.com/docs/auth/admin/verify-id-tokens
 */
final class FirebaseVerifier
{
    private const CERT_URL = 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';
    private const LEEWAY = 60;

    public function __construct(private readonly string $projectId, private readonly string $cacheFile)
    {
    }

    /** @return array{uid:string, provider:string} */
    public function verify(string $token): array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3 || strlen($token) > 4096) {
            throw $this->invalid('Malformed token.');
        }
        [$h64, $p64, $s64] = $parts;
        $header = $this->decodeJson($h64);
        $claims = $this->decodeJson($p64);
        $signature = self::b64urlDecode($s64);
        if ($signature === null || $signature === '') {
            throw $this->invalid('Malformed token.');
        }

        // Pin the algorithm: never trust the header to pick "none" or HS256.
        if (($header['alg'] ?? null) !== 'RS256' || !is_string($header['kid'] ?? null)) {
            throw $this->invalid('Unsupported token.');
        }

        $cert = $this->certificate($header['kid']);
        if ($cert === null || openssl_verify("$h64.$p64", $signature, $cert, OPENSSL_ALGO_SHA256) !== 1) {
            throw $this->invalid('Bad signature.');
        }

        $now = time();
        $iss = 'https://securetoken.google.com/' . $this->projectId;
        if (($claims['iss'] ?? null) !== $iss || ($claims['aud'] ?? null) !== $this->projectId) {
            throw $this->invalid('Token is for another project.');
        }
        if (!is_int($claims['exp'] ?? null) || $claims['exp'] < $now - self::LEEWAY) {
            throw $this->invalid('Token expired.');
        }
        if (!is_int($claims['iat'] ?? null) || $claims['iat'] > $now + self::LEEWAY) {
            throw $this->invalid('Token not valid yet.');
        }
        $uid = $claims['sub'] ?? null;
        if (!is_string($uid) || $uid === '' || strlen($uid) > 128) {
            throw $this->invalid('Token has no subject.');
        }
        $provider = (string) ($claims['firebase']['sign_in_provider'] ?? '');
        // Feedback is for registered accounts only: anonymous sign-ins are not accepted.
        if ($provider === '' || $provider === 'anonymous') {
            throw $this->invalid('A signed-in account is required.');
        }
        return ['uid' => $uid, 'provider' => $provider];
    }

    private function invalid(string $why): ApiException
    {
        return new ApiException(401, 'invalid_token', $why, ['WWW-Authenticate' => 'Bearer error="invalid_token"']);
    }

    /** @return array<string,mixed> */
    private function decodeJson(string $b64): array
    {
        $raw = self::b64urlDecode($b64);
        $data = $raw === null ? null : json_decode($raw, true);
        if (!is_array($data)) {
            throw $this->invalid('Malformed token.');
        }
        return $data;
    }

    private static function b64urlDecode(string $s): ?string
    {
        $out = base64_decode(strtr($s, '-_', '+/'), true);
        return $out === false ? null : $out;
    }

    private function certificate(string $kid): ?string
    {
        $certs = $this->cachedCerts();
        if (!isset($certs[$kid])) {
            // A new signing key may have been rotated in since the cache was written.
            $certs = $this->fetchCerts(true);
        }
        return $certs[$kid] ?? null;
    }

    /** @return array<string,string> */
    private function cachedCerts(): array
    {
        if (is_readable($this->cacheFile)) {
            $c = json_decode((string) file_get_contents($this->cacheFile), true);
            if (is_array($c) && ($c['expires'] ?? 0) > time() && is_array($c['certs'] ?? null)) {
                return $c['certs'];
            }
        }
        return $this->fetchCerts(false);
    }

    /** @return array<string,string> */
    private function fetchCerts(bool $force): array
    {
        // At most one forced refresh a minute, so an attacker cannot make us hammer Google with fake kids.
        if ($force && is_readable($this->cacheFile) && filemtime($this->cacheFile) > time() - 60) {
            $c = json_decode((string) file_get_contents($this->cacheFile), true);
            return is_array($c['certs'] ?? null) ? $c['certs'] : [];
        }
        $ch = curl_init(self::CERT_URL);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER => true,
            CURLOPT_TIMEOUT => 5,
            CURLOPT_CONNECTTIMEOUT => 3,
            CURLOPT_SSL_VERIFYPEER => true,
        ]);
        $resp = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
        curl_close($ch);
        if (!is_string($resp) || $status !== 200) {
            throw new ApiException(503, 'auth_unavailable', 'Cannot verify credentials right now.');
        }
        $certs = json_decode(substr($resp, $headerSize), true);
        if (!is_array($certs) || $certs === []) {
            throw new ApiException(503, 'auth_unavailable', 'Cannot verify credentials right now.');
        }
        $maxAge = 3600;
        if (preg_match('/max-age=(\d+)/i', substr($resp, 0, $headerSize), $m)) {
            $maxAge = (int) $m[1];
        }
        @file_put_contents(
            $this->cacheFile,
            json_encode(['expires' => time() + $maxAge, 'certs' => $certs]),
            LOCK_EX
        );
        return $certs;
    }
}
