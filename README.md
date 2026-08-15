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
    .urlConfig(UrlConfig.from(DashboardHandler.class))
    .authConfig(AuthConfig.builder()
        .basicAuth()
        .user("admin", "secret")
        .build())
    .build());

boot.start("/myapp");
// Server running at http://localhost:8080/myapp/dashboard
```

> [!TIP]
> `./Start-Example.ps1` runs the demo app, which carries a page per feature family —
> `/php-basics`, `/php-loops` and `/php-data`.

---

## Quick Start

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
No annotations, no routing table.

```java
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
WebBoot boot = new WebBoot(WebConfig.builder()
    .port(8080)
    .urlConfig(UrlConfig.from(HomeHandler.class))
    .authConfig(AuthConfig.builder()
        .basicAuth()
        .user("demo", "demo")
        .build())
    .build());

boot.start("/app");
```

That's it. Open `http://localhost:8080/app/home` and you're live.

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
| **Host data** | `$_GET`, `$_POST`, `$_REQUEST`, `$_COOKIE`, `$_SERVER`, plus whatever the handler passes |

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

Anything the dialect does not have produces an error naming what is missing, at load time where possible — never a
silently different page. If a function you need is absent, [open an issue](../../issues); the library is where it
gets added.

---

## Module Architecture

```
druvu-lib-web-api      Pure interfaces & contracts (no runtime dependencies)
druvu-lib-web-core     Jetty 12 server, dispatcher, auth, WebSocket engine
druvu-lib-web-php      PHP template engine plugin (auto-discovered via ServiceLoader)
druvu-lib-web-example  Demo application with dashboard, grid, JSON and WebSocket examples
```

The API module defines all contracts. The core module provides the Jetty-based implementation. The PHP engine is discovered at boot via `ServiceLoader`, so the core never depends on it directly.

---

## Handlers

### HTTP Handlers

Implement `HttpHandler` and register with `UrlConfig.from()`:

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

If the handler does not commit the response (no `commitContent()`, no `sendRedirect()`), the framework automatically forwards to the matching template — e.g. `ProductHandler` forwards to `product.php`.

#### JSON Responses

For API endpoints, commit the response directly — no template is rendered:

```java
public class ApiHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        var data = Map.of("status", "ok", "count", "42");
        response.commitContent("application/json", new Gson().toJson(data));
    }
}
```

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
    .host("0.0.0.0")                          // bind address (default: all interfaces)
    .port(8080)                                // listen port
    .serveFromDirectory(Path.of("/var/www"))    // serve static files from external folder
    .staticPath("/assets/*")                   // additional static resource path
    .globalObject("myService", myService)      // application-scoped object
    .urlConfig(UrlConfig.from(MyHandler.class)) // register handler
    .authConfig(authConfig)                    // authentication config
    .build();
```

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

### Basic Auth with Inline Users

```java
AuthConfig.builder()
    .basicAuth()
    .realm("My Application")
    .sessionTimeout(3600)                // 1 hour (default: 1800s)
    .sessionRenewal(true)                // extend session on activity (default: true)
    .user("admin", "secret", "admin:all", "user:read")
    .user("viewer", "pass", "user:read")
    .build()
```

### Custom UserStore

For production use with databases, LDAP, or any external source — implement the `UserStore` interface:

```java
public interface UserStore {
    Set<String> permissions(String principalName);
}
```

```java
AuthConfig.builder()
    .basicAuth()
    .userStore(new JdbcUserStore(dataSource))
    .build()
```

### Per-URL Permissions

Permissions are defined when registering handlers:

```java
.urlConfig(UrlConfig.from(PublicHandler.class)) // no auth required
.urlConfig(UrlConfig.from(UserHandler.class, "user:read")) // requires user:read
.urlConfig(UrlConfig.from(AdminHandler.class, "admin:all", "user:read")) // requires both
```

The dispatcher checks permissions before invoking the handler. Unauthorized requests receive a 401 response.

### Auth in WebSocket

WebSocket connections authenticate via the `Authorization` header during the upgrade handshake. Once connected, the user identity is available on the session:

```java
public void onConnect(Session session, Sessions sessions) {
    session.user().ifPresent(user ->
        System.out.println("Connected: " + user.getUserPrincipal().getName()));
}
```

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

### From the Classpath

By default, static files are served from `src/main/resources/webapp/static/` at the `/static/*` URL path. WebJar assets are served at `/webjars/*`.

### From an External Directory

Serve files from a folder outside the classpath — useful for development hot-reload or user-uploaded content:

```java
WebConfig.builder()
    .serveFromDirectory(Path.of("/var/www/myapp"))
    // ...
```

Files from the external directory, the `webapp/` classpath resource, and WebJars are all merged into a single resource base.

### Custom Static Paths

Add additional static resource mappings:

```java
WebConfig.builder()
    .staticPath("/assets/*")
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
| `paramInfo()`        | Query/form parameters with typed getters |
| `user()`             | Authenticated user identity (if any)     |
| `globalAttributes()` | Application-scoped objects               |
| `method()`           | HTTP method (`GET`, `POST`)              |
| `contentType()`      | Request content type                     |
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
request.paramInfo().get("q");                            // "java"
request.paramInfo().getOptional("page", Integer::parseInt); // Optional<Integer>(2)
request.paramInfo().getOrDefault("sort", "relevance");   // "relevance"
```

### HttpResponse

| Method | Description |
|---|---|
| `commitContent(contentType, body)` | Write response and commit (skips template) |
| `sendRedirect(url)` | HTTP redirect |
| `sendError(code)` | Send error status |
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
druvu-lib-web-parent/
  druvu-lib-web-api/         # interfaces: HttpHandler, WebSocketHandler, WebConfig, AuthConfig
  druvu-lib-web-core/        # implementation: WebBoot, DispatcherServlet, auth, WebSocket
  druvu-lib-web-php/         # PHP template engine: lexer, parser, syntax tree, runtime, function library
  druvu-lib-web-example/     # demo app: PHP feature pages, w2ui grid, JSON API, WebSocket chat
```

### Tech Stack

| Component        | Technology                                   |
|------------------|----------------------------------------------|
| Server           | Jetty 12.1.6 (EE10 Servlet API)              |
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
