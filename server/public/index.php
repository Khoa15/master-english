<?php
declare(strict_types=1);

require __DIR__ . '/../src/Env.php';
require __DIR__ . '/../src/ApiException.php';
require __DIR__ . '/../src/FirebaseVerifier.php';
require __DIR__ . '/../src/RateLimiter.php';
require __DIR__ . '/../src/Api.php';

DoraDodo\Env::load(__DIR__ . '/../.env');
(new DoraDodo\Api())->handle($_SERVER['REQUEST_METHOD'] ?? 'GET', $_SERVER['REQUEST_URI'] ?? '/');
