<?php
// Prepended to every template when real PHP renders it (auto_prepend_file): the engine's own helpers that
// PHP does not have. The engine escapes echoed values by default and raw() opts out; in PHP nothing is
// escaped, so raw() is the identity and the templates only echo values that escape to themselves.
function raw($value) { return $value; }
