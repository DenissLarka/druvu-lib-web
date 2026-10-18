package com.druvu.web.core.handlers;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.handlers.GlobalAttributes;
import com.druvu.web.api.handlers.HttpMethod;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.ParamInfo;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.api.handlers.UploadedFile;
import com.druvu.web.core.handlers.attr.GlobalAttributesImpl;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/** @author Deniss Larka on 08 June 2024 */
@Slf4j
public final class HttpRequestImpl implements HttpRequest {

    private static final String ANONYMOUS = "ANONYMOUS";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final Optional<AuthUserIdentity> authUserOpt;
    private final PathInfo pathInfo;
    private final ParamInfo paramInfo;
    private final List<UploadedFile> uploads;
    private final DispatcherType dispatcherType;
    private final GlobalAttributes globalAttributes;
    private final HttpServletRequest request;
    private String body;

    public HttpRequestImpl(HttpServletRequest request, Optional<AuthUserIdentity> identityOpt) {
        this.request = Objects.requireNonNull(request);
        this.authUserOpt = Objects.requireNonNull(identityOpt);
        this.globalAttributes = GlobalAttributesImpl.from(request.getServletContext());
        this.pathInfo = new PathInfo(request.getContextPath(), request.getPathInfo(), globalAttributes.defaultPath());
        Map<String, String[]> parameters = new HashMap<>(request.getParameterMap());
        List<UploadedFile> files = new ArrayList<>();
        if (isMultipart(request.getContentType())) {
            readParts(request, parameters, files);
        }
        this.paramInfo = new ParamInfo(parameters);
        this.uploads = List.copyOf(files);
        this.dispatcherType = request.getDispatcherType();
    }

    /**
     * A multipart body: its text fields join the parameters, its files become uploads. A file input left empty sends a
     * part with no file name and no bytes, which is neither.
     */
    private static void readParts(
            HttpServletRequest request, Map<String, String[]> parameters, List<UploadedFile> files) {
        try {
            for (Part part : request.getParts()) {
                String submittedName = part.getSubmittedFileName();
                if (submittedName == null) {
                    parameters.put(part.getName(), new String[] {text(part)});
                } else if (!submittedName.isEmpty() || part.getSize() > 0) {
                    files.add(new PartUpload(part));
                }
            }
        } catch (ServletException | IOException e) {
            log.warn("The multipart body could not be read: {}", e.getMessage());
        }
    }

    private static String text(Part part) throws IOException {
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean isMultipart(String contentType) {
        return contentType != null
                && contentType.regionMatches(true, 0, MULTIPART_FORM_DATA, 0, MULTIPART_FORM_DATA.length());
    }

    @Override
    public void setAttribute(String key, Object value) {
        request.setAttribute(key, value);
    }

    @Override
    public GlobalAttributes globalAttributes() {
        return globalAttributes;
    }

    @Override
    public PathInfo pathInfo() {
        return pathInfo;
    }

    @Override
    public String contentType() {
        return request.getContentType();
    }

    @Override
    public Optional<String> header(String name) {
        return Optional.ofNullable(request.getHeader(name));
    }

    @Override
    public String body() {
        if (body == null) {
            body = readBody();
        }
        return body;
    }

    private String readBody() {
        if (isForm(request.getContentType())) {
            return "";
        }
        try (InputStream in = request.getInputStream()) {
            return new String(in.readAllBytes(), charset());
        } catch (IOException e) {
            throw new UncheckedIOException("The request body could not be read", e);
        }
    }

    private Charset charset() {
        String encoding = request.getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    private static boolean isForm(String contentType) {
        return contentType != null
                && (contentType.regionMatches(true, 0, FORM_URLENCODED, 0, FORM_URLENCODED.length())
                        || contentType.regionMatches(true, 0, MULTIPART_FORM_DATA, 0, MULTIPART_FORM_DATA.length()));
    }

    @Override
    public DispatcherType dispatcherType() {
        return dispatcherType;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.of(request.getMethod());
    }

    @Override
    public String methodName() {
        return request.getMethod();
    }

    @Override
    public boolean isDispatcherTypeRequest() {
        return dispatcherType() == DispatcherType.REQUEST;
    }

    @Override
    public boolean isDispatcherTypeInclude() {
        return dispatcherType() == DispatcherType.INCLUDE;
    }

    @Override
    public String mainPath() {
        return pathInfo.mainPath();
    }

    @Override
    public ParamInfo paramInfo() {
        return paramInfo;
    }

    @Override
    public Optional<UploadedFile> upload(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        return uploads.stream()
                .filter(file -> file.fieldName().equals(fieldName))
                .findFirst();
    }

    @Override
    public List<UploadedFile> uploads() {
        return uploads;
    }

    @Override
    public Object getAttribute(String attributeName) {
        return request.getAttribute(attributeName);
    }

    public Optional<AuthUserIdentity> user() {
        return authUserOpt;
    }

    public String userName() {
        return user().map(AuthUserIdentity::getUserPrincipal)
                .map(Principal::getName)
                .orElse(ANONYMOUS);
    }

    @Override
    public String toString() {
        return pathInfo().toString() + paramInfo().toString() + '@' + userName();
    }
}
