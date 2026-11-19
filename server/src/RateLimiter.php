<?php
declare(strict_types=1);

namespace DoraDodo;

use PDO;

/**
 * Fixed-window counters kept in MySQL, so every PHP worker and server shares them.
 * A client can make up to 2x the limit across a window boundary; that is acceptable for spam control.
 * Keys are hashed with a secret salt before storage: no raw IPs or user ids end up in rate_limits.
 */
final class RateLimiter
{
    public function __construct(private readonly PDO $db, private readonly string $salt)
    {
    }

    /** Counts one hit and throws a 429 when {@code $limit} per {@code $windowSeconds} is exceeded. */
    public function hit(string $scope, string $subject, int $limit, int $windowSeconds): void
    {
        $now = time();
        $windowStart = intdiv($now, $windowSeconds) * $windowSeconds;
        $bucket = $scope . ':' . $windowSeconds . ':' . substr(hash_hmac('sha256', $subject, $this->salt), 0, 32);

        $this->db->prepare(
            'INSERT INTO rate_limits (bucket, window_start, hits) VALUES (?, ?, 1)
             ON DUPLICATE KEY UPDATE hits = hits + 1'
        )->execute([$bucket, $windowStart]);

        $st = $this->db->prepare('SELECT hits FROM rate_limits WHERE bucket = ? AND window_start = ?');
        $st->execute([$bucket, $windowStart]);
        $hits = (int) $st->fetchColumn();

        if ($hits > $limit) {
            $retry = max(1, $windowStart + $windowSeconds - $now);
            throw new ApiException(429, 'rate_limited', 'Too many requests. Try again later.', [
                'Retry-After' => (string) $retry,
            ]);
        }
    }

    /** Drops windows older than two days. Called now and then so the table stays small. */
    public function purge(): void
    {
        $this->db->prepare('DELETE FROM rate_limits WHERE window_start < ?')->execute([time() - 172800]);
    }
}
