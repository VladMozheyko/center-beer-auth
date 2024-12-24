//package fr.mossaab.security.config;
//
//import fr.mossaab.security.entities.ApiResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.MethodParameter;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Objects;
//
///**
// * Глобальный "советчик" для всех REST-ответов.
// */
//@Slf4j
//@ControllerAdvice
//public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {
//
//    /**
//     * Определяем, нужно ли нам вообще обрабатывать ответ.
//     * Сейчас — ко всем JSON-ответам, кроме случаев, когда body = null или
//     * когда MediaType не является JSON-подобным (например, для отдачи файлов).
//     */
//    @Override
//    public boolean supports(MethodParameter returnType,
//                            Class<? extends HttpMessageConverter<?>> converterType) {
//
//        // Можно ввести свою дополнительную логику фильтрации,
//        // например, проверять наличие аннотации @SkipGlobalAdvice
//        // или тип возвращаемого объекта.
//        return true;
//    }
//
//    /**
//     * Метод, который вызывается перед сериализацией ответа.
//     * Здесь мы оборачиваем исходный body в ApiResponse, если он ещё не обёрнут,
//     * и дополняем нужными полями (timestamp, user, path, method и т. д.).
//     */
//    @Override
//    public Object beforeBodyWrite(Object body,
//                                  MethodParameter returnType,
//                                  MediaType selectedContentType,
//                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
//                                  org.springframework.http.server.ServerHttpRequest request,
//                                  org.springframework.http.server.ServerHttpResponse response) {
//
//        // 1. Если body уже ApiResponse — возвращаем как есть.
//        if (body instanceof ApiResponse) {
//            return body;
//        }
//
//        // 2. Проверим, что тип ответа действительно JSON (или совместимый).
//        // Если, к примеру, идёт отдача файла (APPLICATION_OCTET_STREAM),
//        // мы не хотим его оборачивать.
//        if (!isJsonMediaType(selectedContentType)) {
//            return body;
//        }
//
//        // 3. Попытаемся получить HttpServletRequest/HttpServletResponse
//        // для извлечения статуса, пути и т. д.
//        HttpServletRequest servletRequest = getHttpServletRequest();
//        HttpServletResponse servletResponse = getHttpServletResponse();
//
//        // 4. Определим URL и метод:
//        String path = (servletRequest != null) ? servletRequest.getRequestURI() : "";
//        String method = (servletRequest != null) ? servletRequest.getMethod() : "";
//
//        // 5. Сформируем имя пользователя из Spring SecurityContext (если оно есть).
//        // Если не используете Spring Security — можно убрать.
//        String username = getAuthenticatedUser();
//
//        // 6. Если нужно, можно вычислять время обработки запроса:
//        // Для этого на входе (Filter/Interceptor) можно проставлять startTime в request-атрибут.
//        // Здесь посмотрим, есть ли он.
//        String processingTime = null;
//        LocalDateTime now = LocalDateTime.now();
//
//        LocalDateTime startTime = (LocalDateTime) RequestContextHolder.currentRequestAttributes()
//                .getAttribute("requestStartTime", RequestAttributes.SCOPE_REQUEST);
//        if (startTime != null) {
//            long millis = Duration.between(startTime, now).toMillis();
//            processingTime = millis + " ms";
//        }
//
//        // 7. Статус ответа (по умолчанию 200, если не получается вычитать).
//        int status = (servletResponse != null) ? servletResponse.getStatus() : 200;
//
//        // 8. Создаем стандартный ответ
//        ApiResponse<Object> apiResponse = ApiResponse.<Object>builder()
//                .timestamp(now)
//                .status(status)
//                .message("Success")      // Можете менять логику формирования сообщения
//                .path(path)
//                .method(method)
//                .processingTime(processingTime)
//                .user(username)
//                .data(body)
//                .build();
//
//        // 9. Можно добавить логирование
//        log.debug("Wrapped response for path={} method={} status={} user={}",
//                path, method, status, username);
//
//        return apiResponse;
//    }
//
//    /**
//     * Проверка, что MIME-тип соответствует JSON (application/json, application/*+json, etc.).
//     */
//    private boolean isJsonMediaType(MediaType mediaType) {
//        return mediaType != null
//                && (MediaType.APPLICATION_JSON.includes(mediaType)
//                || mediaType.getSubtype().contains("json"));
//    }
//
//    /**
//     * Получаем HttpServletRequest из контекста.
//     */
//    private HttpServletRequest getHttpServletRequest() {
//        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
//        if (attrs instanceof ServletRequestAttributes) {
//            return ((ServletRequestAttributes) attrs).getRequest();
//        }
//        return null;
//    }
//
//    /**
//     * Получаем HttpServletResponse из контекста.
//     */
//    private HttpServletResponse getHttpServletResponse() {
//        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
//        if (attrs instanceof ServletRequestAttributes) {
//            return ((ServletRequestAttributes) attrs).getResponse();
//        }
//        return null;
//    }
//
//    /**
//     * Получаем имя пользователя (если пользователь аутентифицирован).
//     * Если не используете Spring Security, метод можно упростить/убрать.
//     */
//    private String getAuthenticatedUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return "anonymous";
//        }
//
//        Object principal = authentication.getPrincipal();
//
//        // Если principal хранит данные пользователя в виде UserDetails
//        if (principal instanceof UserDetails userDetails) {
//            return userDetails.getUsername();
//        }
//
//        // Часто в простых сценариях principal — это String (например, "anonymousUser")
//        else if (principal instanceof String principalString) {
//            if ("anonymousUser".equalsIgnoreCase(principalString)) {
//                return "anonymous";
//            }
//            return principalString;
//        }
//        // На всякий случай "fallback" — если мы не знаем тип principal
//        return "anonymous";
//    }
//
//}
