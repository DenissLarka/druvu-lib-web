# druvu-lib-web

[![CI](https://github.com/DenissLarka/druvu-lib-web/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-lib-web/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/DenissLarka/druvu-lib-web?label=GitHub%20Packages&color=blue)](https://github.com/DenissLarka/druvu-lib-web/packages)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

**Write your pages in PHP. Run them on the JVM.**

A lightweight Java web framework on Jetty 12, with a PHP template engine — no PHP runtime, no FPM,
no `mod_php`. Layouts are real PHP 8: variables, every control structure, string interpolation, heredocs, includes,
arrow functions and 115 standard functions. The dialect's semantics are settled by execution against PHP 8, 
with whole templates matched against PHP's own output byte for byte.

One thing is deliberately **better** than PHP: **output is HTML-escaped by default**, with `raw()` as the opt-out. In
PHP, every template that forgets `htmlspecialchars` is a hole. Here, forgetting is safe.

Project page: [druvu.com/projects/druvu-lib-web](https://druvu.com/projects/druvu-lib-web.html)

Write the page:

```php
<!-- dashboard.php -->
<?php require 'includes/header.php'; ?>
<h1>Welcome, <?= $customer->name ?></h1>

<ul>
<?php foreach ($orders as $order): ?>
  <li><?= $order->reference ?> — <?= number_format($order->total, 2) ?></li>
<?php endforeach; ?>
</ul>
```

Give it something to show:

```java
public class DashboardHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("customer", customer);  // becomes $customer
        request.setAttribute("orders", orders);      // becomes $orders
    }
}
```

Start the server:

```java
WebBoot boot = new WebBoot(WebConfig.builder()
    .port(8080)
    .route(Route.forEveryone(DashboardHandler.class))
    .authConfig(AuthConfig.builder()
        .basicAuth()
        .user("admin", "secret")
        .build())
    .build());

boot.start("/myapp");
// Server running at http://localhost:8080/myapp/dashboard
```

> [!TIP]
> `./Start-Example.ps1` runs the demo site, which carries a page per feature family —
> `/php-basics`, `/php-loops` and `/php-data` — plus `/account` behind a sign-in (`user` / `pass`).
> `./Start-Example.ps1 mcp` runs an MCP server for machines and prints its token; `./Start-Example.ps1 react` runs a
> React app, built by Vite inside the Maven build, on `http://localhost:8083/`.

---

## Quick Start

Add `druvu-lib-web-core` and `druvu-lib-web-php` (the template engine) to your build; both come from GitHub Packages,
see [Installation](#installation). The library needs Java 25 or newer.

### 1. Write the page

Place `home.php` in `src/main/resources/webapp/`:

```php
<!DOCTYPE html>
<html>
<body>
    <h1>Hello, <?= $visitor ?>!</h1>
    <p>You are on <?= context() ?>.</p>
</body>
</html>
```

### 2. Write the handler

A handler's class name gives it both its URL and its template: `HomeHandler` answers `/home` and renders `home.php`.
No annotations, no routing table. The first route given to `WebConfig` also answers the context root.

```java
// src/main/java/com/example/HomeHandler.java
package com.example;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

public class HomeHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("visitor", "Ada");  // becomes $visitor

        // business logic here; if the response is not committed,
        // the framework forwards to home.php
    }
}
```

### 3. Boot the server

```java
// src/main/java/com/example/App.java
package com.example;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;

public class App {
    public static void main(String[] args) {
        WebBoot boot = new WebBoot(WebConfig.builder()
            .port(8080)
            .route(Route.forEveryone(HomeHandler.class))
            .authConfig(AuthConfig.builder()
                .basicAuth()
                .user("demo", "demo")
                .build())
            .build());

        boot.start("/app");  // the context path; start("/") serves the application at the server's root
    }
}
```

That's it. Run `App` and open `http://localhost:8080/app/home`.

### Where the classes live

| Package | Classes |
|---|---|
| `com.druvu.web.core` | `WebBoot` |
| `com.druvu.web.api.config` | `WebConfig`, `Route` |
| `com.druvu.web.api.handlers` | `HttpHandler`, `HttpRequest`, `HttpResponse`, `HttpMethod`, `PathInfo`, `ParamInfo`, `UploadedFile`, `DownloadFile`, `GlobalAttributes`, `WebSocketHandler` (its `Session` and `Sessions` are nested in it) |
| `com.druvu.web.api.auth` | `AuthConfig`, `BearerAuthentication`, `OpenIdAuthentication`, `TokenStore`, `PermissionStore`, `ApiTokens`, `AuthUserIdentity` |
| `com.druvu.web.api.plugin` | `TemplateEnginePlugin` |
| `com.druvu.php` | `PhpEngine`, `TemplateLoader`, `PhpEngineConfig`, `HostRequest`: the engine on its own, artifact `druvu-lib-php` |

---

## PHP Templates

A PHP dialect for laying out HTML, implemented in Java. No PHP runtime is involved and none is needed.

It is a **layout** language, not a scripting one. Everything PHP offers for producing a page is here — variables,
every control structure, expressions, string interpolation, includes, a standard-function subset. Everything else is
deliberately absent: no database access, no filesystem, no network, no sessions, no `eval`, no user-defined functions
or classes. Those belong to the Java application, which is the thing that should be deciding them.

### Output is escaped by default

**This is the one place the dialect deliberately does not behave like PHP.** Anything reaching `echo`, `print` or
`<?= ?>` is HTML-escaped unless you say otherwise:

```php
<?php $bio = "<script>alert('x')</script>"; ?>
<?= $bio ?>              <!-- &lt;script&gt;alert(&#039;x&#039;)&lt;/script&gt; -->
<?= raw($bio) ?>         <!-- printed as-is, because you asked -->
```

In PHP the default is the other way round, and every template that forgets `htmlspecialchars` is a hole. Here
forgetting is safe, and `raw()` is the thing you write on purpose — which also makes it the thing you can search for.
The template's own markup is never touched; only values are. Safety does not survive concatenation: `raw($a) . $b`
gives an ordinary, escaped string.

`link()`, `webjar()`, `context()`, `htmlspecialchars()` and `nl2br()` already return output-safe values, so URLs and
markup-producing helpers need no `raw()`.

### The language

| | |
|---|---|
| **Tags** | `<?php … ?>`, `<?= … ?>`, closing tag optional at end of file, one newline after `?>` swallowed |
| **Values** | int, float, string, bool, null, arrays (list and keyed, nested) |
| **Strings** | single- and double-quoted, `"$name"` and `"{$a['k']}"` interpolation, heredoc and nowdoc |
| **Operators** | full PHP 8 precedence: `** * / % + - .` then comparison, `&& || and or xor`, `?? ?: ? :`, `= += .= ??=`, `++ --`, casts |
| **Control** | `if`/`elseif`/`else`, `foreach`, `for`, `while`, `do…while`, `switch`, `match`, `break n`, `continue n`, `return` |
| **Alternative syntax** | `if (…): … endif;` and the rest — the form a layout is actually written in |
| **Composition** | `include`, `require`, `*_once`; the partial shares the including page's variables |
| **Functions** | **115** built in: escaping, strings, arrays, sorting, type checks, date display, maths |
| **Closures** | arrow functions `fn($x) => …`, for `array_map` and friends |
| **Host data** | `$_GET`, `$_POST`, `$_REQUEST`, `$_COOKIE`, `$_SERVER` (with `REMOTE_USER` = the signed-in subject), plus whatever the handler passes |

```php
<?php $items = ["pen" => 2, "cup" => 5]; ?>
<ul>
<?php foreach ($items as $what => $count): ?>
  <li><?= $count ?> x <?= $what ?></li>
<?php endforeach; ?>
</ul>
```

### Data from the application

A handler runs before the template, and whatever it puts on the request arrives as an ordinary variable of the same
name. There is no model object to reach through:

```java
public void handle(HttpRequest request, HttpResponse response) {
    request.setAttribute("order", order);     // becomes $order
    request.setAttribute("title", "Orders");  // becomes $title
}
```

```php
<?= $order->reference ?>              <!-- a record or bean, read-only -->
<?= $order->customer?->name ?>        <!-- null-safe -->
<?php foreach ($order->lines as $line): ?>…<?php endforeach; ?>
```

Maps become arrays, collections become lists, and anything else becomes a **read-only** view. A property is read from
a no-argument method of that name — which is what a record component is — then `getX()`, then `isX()`. A template can
look at your objects and can do nothing else to them: no setter, no method with arguments, no way past what it was
shown.

Attribute names that are not legal PHP variable names are skipped, which is what keeps the container's own
`jakarta.servlet.*` bookkeeping out of template scope.

The demo app carries a worked page per family — `/php-basics`, `/php-loops`, `/php-data`. Run it with
`./Start-Example.ps1`.

### Built-in helpers for this framework

| Function | Description | Example |
|---|---|---|
| `webjar(path)` | Resolves a WebJar asset to a URL with the context path | `<?= webjar('w2ui-2.0.min.css') ?>` |
| `context()` | The servlet context path | `<?= context() ?>` |
| `link(target)` | A URL relative to the context path | `<?= link('dashboard') ?>` |
| `raw(value)` | Output without escaping | `<?= raw($html) ?>` |

### Where it differs from PHP, on purpose

- **Output is escaped by default** (above).
- **Lengths count characters, not bytes.** `strlen("café")` is 4, not 5; `mb_strlen` is the same function.
- **`date()` formats in UTC.** A layout language has no business guessing a timezone — convert before passing the
  value in.
- **`htmlentities()` writes numeric entities** (`&#233;`) rather than named ones. Browsers render them identically.
- **Numbers are decimal only.** `0755`, `0x1A` and `0b1010` are refused rather than quietly misread, and `.5` is not
  a literal, so `.` is always concatenation.
- **No references, no bitwise operators, no `@` suppression.** The sorts (`sort`, `usort`, …) rearrange the variable
  you give them, and are the only exception.
- **The dump functions are off.** `print_r`, `var_export` and `var_dump` print in the engine's own format, and only when
  the host turns the debug functions on; the servlet does not. A page that needs a value as text uses `json_encode`.

Anything the dialect does not have produces an error naming what is missing, at load time where possible — never a
silently different page. If a function you need is absent, [open an issue](../../issues); the library is where it
gets added.

---

### The engine on its own

The engine has no dependencies and no opinion about HTTP. `druvu-lib-php` renders a template against a map, which is
enough for a mail body, a report, or a template inside another web framework:

```java
PhpEngine engine = new PhpEngine(TemplateLoader.classpath("templates"), PhpEngineConfig.DEFAULTS);
String body = engine.render("/welcome.php", Map.of("name", "Ada")).orElseThrow();
```

`TemplateLoader.directory(path)` reads from disk instead, and an implementation of `HostRequest` — five methods — gives
`$_GET`, `$_POST`, `$_COOKIE`, `$_SERVER` and `context()` a request to read from. `druvu-lib-web-php` is exactly that
implementation over a servlet.

---

## Module Architecture

```
druvu-lib-php                The PHP 8 layout engine on its own: no dependencies, one exported package
druvu-lib-web-api            Pure interfaces & contracts (no runtime dependencies)
druvu-lib-web-core           Jetty 12 server, dispatcher, auth, WebSocket engine
druvu-lib-web-php            The engine as a servlet: the plugin the core discovers via ServiceLoader
druvu-lib-web-example-web    The demo site: PHP feature pages, w2ui grid, JSON, WebSocket, a page behind sign-in
druvu-lib-web-example-mcp    An MCP server on a route for machines, JSON through druvu-lib-json, a token at boot
druvu-lib-web-example-react  A React app (Vite, TypeScript) built inside the Maven build, served as static files
```

The API module defines all contracts. The core module provides the Jetty-based implementation. The PHP engine is discovered at boot via `ServiceLoader`, so the core never depends on it directly.

---

## Handlers

### HTTP Handlers

Implement `HttpHandler` and register it as a `Route`:

```java
public class ProductHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        // Access path segments: /product/42 -> pathInfo.pathOpt(1) = "42"
        Optional<Integer> id = request.pathInfo().pathOpt(1, Integer::parseInt);

        // Access query parameters: ?sort=name
        String sort = request.paramInfo().getOrDefault("sort", "id");

        // Access authenticated user
        Optional<AuthUserIdentity> user = request.user();

        // Access application-scoped objects
        MyService service = request.globalAttributes().get("myServiceKey");
    }
}
```

If the handler does not commit the response (no `commit()`, no `sendRedirect()`), the framework automatically forwards to the matching template — e.g. `ProductHandler` forwards to `product.php`.

#### JSON Responses

For API endpoints, commit the response directly — no template is rendered:

```java
public class ApiHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        var data = Map.of("status", "ok", "count", "42");
        response.commit("application/json", new Gson().toJson(data));
    }
}
```

#### Files: uploads and downloads

A file sent from a form (`<input type="file">` in a `multipart/form-data` form) arrives as an `UploadedFile`, apart
from the form's text fields; a handler sends a file back with `commit(DownloadFile)`:

```java
// a file sent as <input type="file" name="report">; filename() has no directory part, so it is safe in a folder of yours
request.upload("report").ifPresent(file -> file.saveTo(incoming.resolve(file.filename())));

// a file for the browser to save, under any name
response.commit(DownloadFile.of(Path.of("dist/app-1.0.0.dmg")));            // streamed from disk
response.commit(DownloadFile.of("orders.csv", "text/csv", csvBytes));      // made in memory
```

A multipart body over the upload limit (10 MB, or `WebConfig.builder().uploadLimit(bytes)`) never reaches its handler:
it is answered 413, or 400 when it came in chunks without a declared length.

### WebSocket Handlers

Real-time communication with JSON messaging:

```java
public class ChatHandler implements WebSocketHandler {
    @Override
    public void onConnect(Session session, Sessions sessions) {
        // new client connected; sessions.all() gives all active sessions
    }

    @Override
    public void handle(Session session, Sessions sessions, Map<String, String> message) {
        // broadcast to everyone
        for (Session s : sessions.all()) {
            s.send(Map.of("from", session.id(), "text", message.get("text")));
        }
    }

    @Override
    public void onClose(Session session, Sessions sessions) {
        // client disconnected
    }
}
```

WebSocket sessions support per-session attributes, user identity, and open/close state checks.

### Handler Naming Convention

Handler class names are automatically mapped to URL paths:

| Class Name           | URL Path         |
|----------------------|------------------|
| `DashboardHandler`   | `/dashboard`     |
| `UserProfileHandler` | `/user-profile`  |
| `ExampleTable`       | `/example-table` |
| `API`                | `/api`           |

The `Handler` suffix is stripped, then CamelCase is converted to kebab-case. No annotations, no XML — just name your class and it's routed.

---

## Configuration

### WebConfig

```java
WebConfig.builder()
    .host("0.0.0.0")                               // bind address; without it, loopback only
    .port(8080)                                    // listen port; 0 picks a free one, 8080 without it
    .serveFromDirectory(Path.of("/var/www"))       // static files from a directory, in front of the classpath
    .staticPath("/assets/*")                       // one more static pattern; /static/* and /webjars/* stay
    .globalObject("myService", myService)          // reachable from every handler and template
    .route(Route.forEveryone(MyHandler.class))     // the first route also answers the context root
    .authConfig(authConfig)                        // optional: without it, every route is public
    .uploadLimit(50L * 1024 * 1024)                // largest multipart body accepted; 10 MB without it
    .build();
```

`WebBoot` starts the server under a context path. It stops with the JVM, or earlier through `stop()`; `port()` tells
which port was bound, the news when the configuration said 0.

### Global Objects

Share application-scoped objects across all handlers:

```java
DataSource ds = createDataSource();
MyService service = new MyService(ds);

WebConfig.builder()
    .globalObject("myService", service)
    // ...

// In any handler:
public void handle(HttpRequest request, HttpResponse response) {
    MyService svc = request.globalAttributes().get("myService");
}
```

---

## Authentication & Authorization

Sign-in runs in Jetty's own security handler, before any handler of yours: an anonymous visitor to a protected route is
challenged or sent to the provider by Jetty, and what reaches your code is either public or already signed in. Without
an `AuthConfig`, every route is public.

Three ways in:

| Who | How | When |
|---|---|---|
| People | Basic authentication with inline users | demos, tests, a tool behind a VPN |
| People | OpenID Connect through one provider | production: Google alone, or a broker fronting Google, GitHub and Apple |
| Machines | a bearer token in the `Authorization` header | a desktop app or a script calling the application's API |

### Routes say who may reach them

```java
.route(Route.forEveryone(HomeHandler.class))                                // public
.route(Route.forSignedIn(AccountHandler.class))                             // any signed-in person
.route(Route.forSignedIn(OrdersHandler.class, "orders:read"))               // signed in, with this permission
.route(Route.forSignedIn(AdminHandler.class, "admin:all", "orders:read"))   // both permissions
.route(Route.forMachines(GenerateHandler.class, "ea:generate"))             // a bearer token, with this permission
```

Anonymous on a protected route: 401 with a challenge under Basic, a redirect to the provider under OpenID. Signed in
without one of the permissions: 403. A route for machines never takes a browser sign-in, and a people route never
takes a token.

### People: Basic authentication

```java
AuthConfig.builder()
    .basicAuth("My Application")
    .user("admin", "secret", "admin:all", "orders:read")
    .user("viewer", "pass", "orders:read")
    .build()
```

The inline users double as the permission store. Sessions default to 30 minutes idle and a cookie named `session`;
`.sessionTimeout(Duration.ofHours(8))` and `.cookieName("shop")` change that.

### People: OpenID Connect

```java
AuthConfig.builder()
    .people(OpenIdAuthentication.of(
        "https://accounts.google.com",                              // the issuer; the rest is discovered
        clientId, clientSecret))
    .permissions(subject -> repository.permissionsOf(subject))      // what a signed-in subject may do
    .build()
```

The provider sends the browser back to `/auth/callback` under your context, `/auth/logout` ends the session, and the
`email` and `profile` scopes fill the identity's claims; all of it is configurable on `OpenIdAuthentication`. The
permission store is asked once per sign-in and the answer travels with the session, so a database-backed store is not
hit on every request. A subject the store does not know is signed in with no permissions, which is how registration
works: the handler of a signed-in route sees an unknown subject and creates its row. No password ever reaches the
application.

```java
public void handle(HttpRequest request, HttpResponse response) {
    AuthUserIdentity user = request.user().orElseThrow();   // on a signed-in route it is always there
    user.subject();                                         // the provider's stable id
    user.email(); user.displayName();                       // Optional: the claims, when the provider sent them
    user.getPermissions();
}
```

Templates see the subject as `$_SERVER['REMOTE_USER']`.

### Machines: bearer tokens

```java
AuthConfig.builder()
    .people(…)
    .permissions(…)
    .machines(new BearerAuthentication(presented -> repository.subjectOf(ApiTokens.hash(presented))))
    .build()
```

A route registered with `Route.forMachines(…)` requires `Authorization: Bearer <token>`; the token store answers
which subject the token belongs to, and the permission store what that subject may do. The application hands tokens
out and keeps only their hash:

```java
String token = ApiTokens.generate();                     // 256 random bits, URL-safe; show it once
repository.save(user.subject(), ApiTokens.hash(token));
```

A machine gets 401 with `WWW-Authenticate: Bearer` without a valid token, and 403 with one whose subject lacks the
permission. No session is created for a machine.

### Auth in WebSocket

A WebSocket upgrade passes through the same gate as any request: the session cookie, a bearer token or a Basic header
on the upgrade request. Once connected, the user identity is available on the session:

```java
public void onConnect(Session session, Sessions sessions) {
    session.user().ifPresent(user ->
        System.out.println("Connected: " + user.subject()));
}
```

---

## An MCP server on a route for machines

`druvu-lib-web-example-mcp` is an MCP server over Streamable HTTP, protocol `2025-06-18`: one `Route.forMachines(McpHandler.class)`,
a bearer token generated and printed at boot, JSON-RPC in a POST body and JSON back. No sessions and no server stream,
so GET and DELETE answer 405; a foreign `Origin` answers 403, an unknown `MCP-Protocol-Version` 400. Its druvu
dependencies are core, api and [druvu-lib-json](https://github.com/DenissLarka/druvu-lib-json), which builds the
envelopes and reads the requests; no template engine. Two tools, `echo` and `time_now`. Run it and point a client at it:

```bash
./Start-Example.ps1 mcp                                              # prints the token
./druvu-lib-web-examples/druvu-lib-web-example-mcp/Invoke-McpExample.ps1 -Token <token>   # handshake, tool list, both tools
claude mcp add --transport http druvu-example http://localhost:8082/mcp --header "Authorization: Bearer <token>"
```

Or by hand, one JSON-RPC message per POST:

```bash
curl -s -X POST http://localhost:8082/mcp -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"echo","arguments":{"text":"hi"}}}'
```

The server is three small classes: `McpTransport` reads a request into an `McpCall`, or into the `Reply` it already
knows for another method, a foreign origin, an unknown version, unreadable JSON or a notification; `McpServer` answers a
call; a `Reply` writes itself. The handler is a `switch` over the two.

An application with machine routes only needs no browser side: `AuthConfig.builder().machines(...)` alone is a way in,
and `WebConfig.build()` refuses a route whose audience has none — a signed-in route without people authentication, a
machine route without `machines(...)`.

---

## WebJars Integration

Use any JavaScript or CSS library from [webjars.org](https://www.webjars.org/) — just add the Maven dependency and reference it with `webjar()`:

**1. Add the dependency:**

```xml
<dependency>
    <groupId>org.webjars.npm</groupId>
    <artifactId>w2ui</artifactId>
    <version>2.0.0</version>
</dependency>
```

**2. Use it in templates:**

```php
<link rel="stylesheet" href="<?= webjar('w2ui-2.0.min.css') ?>" />

<script type="module">
    import { w2grid } from '<?= webjar('w2ui-2.0.es6.min.js') ?>';

    new w2grid({
        box: '#grid',
        name: 'myGrid',
        columns: [
            { field: 'recid', text: 'ID', size: '60px' },
            { field: 'name',  text: 'Name', size: '40%' }
        ],
        records: [
            { recid: 1, name: 'Alice' },
            { recid: 2, name: 'Bob' }
        ]
    }).render();
</script>
```

The `webjar()` function uses `WebJarAssetLocator` to resolve partial file names to their full versioned path, so you never hardcode version numbers in templates.

---

## Serving Static Files

A static pattern is served at its own path from one merged base: the classpath's `webapp/`, every jar's
`META-INF/resources/` (where WebJars keep their files), and a directory on disk when you give one. The base is laid out
like the URL space, so `/static/app.css` is `webapp/static/app.css`, and `/webjars/w2ui/2.0.0/w2ui-2.0.min.css` is
that file inside the w2ui jar.

### From the Classpath

Two patterns are always on: `/static/*` for `src/main/resources/webapp/static/`, and `/webjars/*` for WebJar assets.

### From a Directory

A directory outside the classpath comes first in the merged base — for development without a rebuild, or for content
users upload. It follows the same layout: `/static/logo.png` is `/var/www/myapp/static/logo.png`.

```java
WebConfig.builder()
    .serveFromDirectory(Path.of("/var/www/myapp"))
    // ...
```

### More Static Patterns

One more pattern serves the matching folder of the base; the two defaults stay:

```java
WebConfig.builder()
    .staticPath("/assets/*")     // webapp/assets/ on the classpath, assets/ in the directory
    .staticPath("/uploads/*")
    // ...
```

---

## Template Engine Discovery

The PHP engine is not compiled into the core. It is discovered at boot through `ServiceLoader` (via
`druvu-lib-loader`), which is why `druvu-lib-web-core` has no dependency on `druvu-lib-web-php`. The contract is two
methods:

```java
public interface TemplateEnginePlugin {
    void registerServlet(Object handler);  // the Jetty ServletContextHandler — Object keeps the api module Jetty-free
    String getName();                      // for logging
}
```

A plugin registers its servlet under the name the dispatcher forwards to. **One engine ships** — `PHP Engine`,
registered as `php` — and the dispatcher's extension is a constant matching it. A second engine would mean turning
that constant back into a lookup; nothing here pretends it already is one.

---

## Request & Response API

### HttpRequest

| Method               | Description                              |
|----------------------|------------------------------------------|
| `pathInfo()`         | URL segments with typed extraction       |
| `paramInfo()`        | Query/form parameters with typed getters; a multipart form's text fields |
| `upload(name)`       | The file sent under this form field, if any |
| `uploads()`          | Every file of a multipart body, in the order sent |
| `user()`             | Authenticated user identity (if any)     |
| `globalAttributes()` | Application-scoped objects               |
| `method()`           | The method as `HttpMethod`, an enum: `GET`, `POST`, ... |
| `isGet()`, `isPost()` | On the request itself: `request.isPost()` is true for a POST |
| `methodName()`       | The method as it came on the wire        |
| `setAttribute(name, value)` | Hands a value to the template, where it is `$name` |
| `contentType()`      | Request content type                     |
| `header(name)`       | A request header, if present             |
| `body()`             | The body as text, UTF-8 unless declared; empty for a form body |
| `mainPath()`         | First URL segment (handler name)         |

### PathInfo

```java
// URL: /product/42/details
request.pathInfo().mainPath();                          // "product"
request.pathInfo().pathOpt(1, Integer::parseInt);       // Optional<Integer>(42)
request.pathInfo().getOrDefault(2, "overview");          // "details"
request.pathInfo().withContextPath("other");             // "/myapp/other"
```

### ParamInfo

```java
// URL: /search?q=java&page=2
request.paramInfo().get("q");                               // "java"
request.paramInfo().get("sort");                            // null: absent, and a blank value counts as absent
request.paramInfo().getOptional("q");                       // Optional["java"]
request.paramInfo().getOptional("page", Integer::parseInt); // Optional<Integer>(2)
request.paramInfo().getOrDefault("sort", "relevance");      // "relevance"
```

### HttpResponse

| Method | Description |
|---|---|
| `header(name, value)` | Set a response header, before committing |
| `commit(contentType, body)` | Commit 200 with this body, UTF-8; no template runs |
| `commit(status, contentType, body)` | The same with another status |
| `commit(status)` | Commit a bare status: 202, 204, 405 with `Allow` set before |
| `commit(DownloadFile.of(...))` | Commit 200 with a file for the browser to save, any file name intact |
| `sendRedirect(url)` | Redirect (302) to this URL; no template runs |
| `sendRedirect(AccountHandler.class)` | Redirect (302) to that handler's route in this application, from any depth; a class that is no route throws `IllegalArgumentException` |
| `sendError(code)` | Send error status through the error handler |
| `isCommitted()` | Check if response was already written |

---

## Build & Test

```bash
mvn compile         # compile all modules
mvn test            # run tests (TestNG)
mvn package         # full build with Javadoc + sources
```

**Requirements:** Java 25, Maven 3.9+

---

## Project Structure

```
druvu-lib-web/
  druvu-lib-web-api/         # interfaces: HttpHandler, WebSocketHandler, WebConfig, Route, AuthConfig
  druvu-lib-web-core/        # implementation: WebBoot, DispatcherServlet, security, WebSocket
  druvu-lib-php/             # PHP engine: lexer, parser, syntax tree, runtime, function library
  druvu-lib-web-php/         # the engine as a servlet, plugged into the core
  druvu-lib-web-examples/            # runnable starting points, nothing published
    druvu-lib-web-example-web/       # the demo site: PHP feature pages, w2ui grid, JSON, WebSocket, sign-in
    druvu-lib-web-example-mcp/       # an MCP server over Streamable HTTP, JSON through druvu-lib-json
```

### Tech Stack

| Component        | Technology                                   |
|------------------|----------------------------------------------|
| Server           | Jetty 12.1 (EE10 Servlet API)                |
| Language         | Java 25                                      |
| Build            | Maven                                        |
| Template Engine  | PHP dialect implemented in Java              |
| WebSocket        | Jetty WebSocket API, JSON messaging via GSON |
| Asset Management | WebJars + WebJarAssetLocator                 |
| Engine Discovery | ServiceLoader (via druvu-lib-loader)         |
| Testing          | TestNG                                       |

---

## Installation

This library is published to **GitHub Packages**.

### 1. Generate a GitHub Personal Access Token

Go to [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens) and create a token with the `read:packages` scope.

### 2. Add the server to `~/.m2/settings.xml`

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

### 3. Add the repository and dependencies to your project `pom.xml`

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DenissLarka/druvu-lib-web</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-web-core</artifactId>
    <version>2.0.0</version>
</dependency>
<!-- PHP template engine (loaded automatically via ServiceLoader) -->
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-web-php</artifactId>
    <version>2.0.0</version>
    <scope>runtime</scope>
</dependency>
```

---

## License

See [LICENCE](LICENSE) for details.
