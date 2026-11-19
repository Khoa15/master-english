<?php
declare(strict_types=1);

namespace DoraDodo;

/** Reads settings from real environment variables, falling back to server/.env. */
final class Env
{
    /** @var array<string,string> */
    private static array $file = [];

    public static function load(string $path): void
    {
        if (!is_readable($path)) {
            return;
        }
        foreach (file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
            $line = trim($line);
            if ($line === '' || $line[0] === '#' || !str_contains($line, '=')) {
                continue;
            }
            [$k, $v] = explode('=', $line, 2);
            self::$file[trim($k)] = trim($v);
        }
    }

    public static function get(string $key, ?string $default = null): ?string
    {
        $real = getenv($key);
        if ($real !== false && $real !== '') {
            return $real;
        }
        return self::$file[$key] ?? $default;
    }

    public static function int(string $key, int $default): int
    {
        $v = self::get($key);
        return $v !== null && ctype_digit($v) ? (int) $v : $default;
    }

    public static function require(string $key): string
    {
        $v = self::get($key);
        if ($v === null || $v === '') {
            throw new \RuntimeException("Missing configuration: $key");
        }
        return $v;
    }
}
