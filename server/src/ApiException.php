<?php
declare(strict_types=1);

namespace DoraDodo;

/** An error the client is allowed to see: HTTP status, stable machine code, short message. */
final class ApiException extends \Exception
{
    /** @param array<string,string> $headers */
    public function __construct(
        public readonly int $status,
        public readonly string $errorCode,
        string $message,
        public readonly array $headers = []
    ) {
        parent::__construct($message);
    }
}
