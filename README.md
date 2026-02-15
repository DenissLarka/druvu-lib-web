# druvu-lib-web

A lightweight Java web framework built on Jetty. Define handlers, add auth, pick a template engine — your app is running in under 20 lines.

> [!NOTE]
> **About the PHP template engine** — Yes, this is a PHP implementation in Java. It is not meant to be a full PHP runtime — it covers simple web layout tasks like includes, string expressions, and built-in helper functions. If you need additional PHP functionality supported, I'd love to hear about it! Feel free to [open an issue](../../issues) with your use case or feature request.

```java
WebBoot boot = new WebBoot(WebConfig.builder()
    .port(8080)
    .urlConfig(UrlConfig.from(DashboardHandler.class))
    .urlConfig(UrlConfig.from(UsersHandler.class, "admin:access"))
    .authConfig(AuthConfig.builder()
        .basicAuth()
        .user("admin", "secret", "admin:access")
        .build())
    .build());

boot.start("/myapp");
// Server running at http://localhost:8080/myapp/
```
> [!TIP]
> You can launch the example app with `./run-example.ps1`.


```java
public class DashboardHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        // handler logic runs, then forwards to the dashboard.php template
    }
}
```

```php
<!-- dashboard.php -->
<?php require 'includes/header.php'; ?>
<link rel="stylesheet" href="<?= webjar('bootstrap/css/bootstrap.min.css') ?>" />
<h1>Welcome, <?= context() ?></h1>
<a href="<?= link('users') ?>">Manage Users</a>
<?php require 'includes/footer.php'; ?>
```

---

## Quick Start

### 1. Add dependencies

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-web-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<!-- PHP template engine (loaded automatically via ServiceLoader) -->
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-web-php</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope>
</dependency>
```

### 2. Create a handler

```java
public class HomeHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        // business logic here
        // if response is not committed, the framework forwards to home.php
    }
}
```

### 3. Create a template

Place `home.php` in `src/main/resources/webapp/`:

```php
<!DOCTYPE html>
<html>
<body>
    <h1>Hello from <?= context() ?>!</h1>
</body>
</html>
```

### 4. Boot the server

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

## Module Architecture

```
druvu-lib-web-api      Pure interfaces & contracts (no runtime dependencies)
druvu-lib-web-core     Jetty 12 server, dispatcher, auth, WebSocket engine
druvu-lib-web-php      PHP template engine plugin (auto-discovered via ServiceLoader)
druvu-lib-web-example  Demo application with dashboard, grid, JSON and WebSocket examples
```

The API module defines all contracts. The core module provides the Jetty-based implementation. Template engines are plugins — drop a jar on the classpath and it's picked up automatically.

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
    .templateSystem("php")                     // template engine (default: "php")
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
.urlConfig(UrlConfig.from(PublicHandler.class))                        // no auth required
.urlConfig(UrlConfig.from(UserHandler.class, "user:read"))             // requires user:read
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

## PHP Template Engine

A Java-based PHP-like template engine discovered automatically via ServiceLoader. No PHP runtime needed — it's pure Java parsing.

### Supported Syntax

```php
<?php echo 'Hello ' . 'World'; ?>     <!-- echo tag -->
<?= 'Short echo: ' . context() ?>     <!-- short echo tag -->
<?php require 'includes/header.php'; ?> <!-- include files -->
<?php require_once 'lib/utils.php'; ?> <!-- include once (prevents circular includes) -->
```

### Expression Support

| Feature            | Example                                |
|--------------------|----------------------------------------|
| String literals    | `'hello'`, `"world"`                   |
| Concatenation      | `'Hello' . ' ' . 'World'`              |
| Function calls     | `webjar('jquery.min.js')`              |
| Nested expressions | `'prefix-' . link('page') . '-suffix'` |
| Escape sequences   | `'\n'`, `'\t'`, `'\\'`                 |

### Built-in Functions

| Function       | Description                                             | Example                             |
|----------------|---------------------------------------------------------|-------------------------------------|
| `webjar(path)` | Resolves a WebJar asset to a full URL with context path | `<?= webjar('w2ui-2.0.min.css') ?>` |
| `context()`    | Returns the servlet context path                        | `<?= context() ?>`                  |
| `link(target)` | Builds a URL relative to the context path               | `<?= link('dashboard') ?>`          |

### Extensibility

The function system is pluggable. Custom functions can be registered on the `PhpFunctionRegistry`. The expression parser is designed for extension — variable support (`$var`), control structures (`if`/`foreach`), and superglobals (`$_GET`, `$_POST`) are planned.

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

## Plugin System

Template engines are loaded via `ServiceLoader` through the `TemplateEnginePlugin` interface:

```java
public interface TemplateEnginePlugin {
    void registerServlet(Object handler);      // register with Jetty context
    String[] getSupportedExtensions();         // e.g. ["php"]
    String getName();                          // for logging
    default int getPriority() { return 0; }   // higher wins on conflict
}
```

Drop a new template engine jar on the classpath with a `META-INF/services` entry or a `module-info.java` `provides` declaration, and it's picked up at boot. The `templateSystem` config selects which engine to use (default: `"php"`).

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
  druvu-lib-web-php/         # PHP template engine: tokenizer, expression parser, functions
  druvu-lib-web-example/     # demo app: dashboard, w2ui grid, JSON API, WebSocket chat
```

### Tech Stack

| Component        | Technology                                   |
|------------------|----------------------------------------------|
| Server           | Jetty 12.1.6 (EE10 Servlet API)              |
| Language         | Java 25                                      |
| Build            | Maven                                        |
| Template Engine  | PHP-like (Java-based, pluggable)             |
| WebSocket        | Jetty WebSocket API, JSON messaging via GSON |
| Asset Management | WebJars + WebJarAssetLocator                 |
| Plugin Discovery | ServiceLoader (via druvu-lib-loader)         |
| Testing          | TestNG                                       |

---

## License

See [LICENCE](LICENSE) for details.
