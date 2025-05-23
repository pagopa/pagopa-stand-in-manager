package it.gov.pagopa.standinmanager.config;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

  public static final String START_TIME = "startTime";
  public static final String METHOD = "method";
  public static final String STATUS = "status";
  public static final String CODE = "httpCode";
  public static final String RESPONSE_TIME = "responseTime";

  @Autowired HttpServletRequest httRequest;

  @Autowired HttpServletResponse httpResponse;

  @Value("${info.application.artifactId}")
  private String artifactId;

  @Value("${info.application.version}")
  private String version;

  @Value("${info.properties.environment}")
  private String environment;

  private static String getExecutionTime() {
    String startTime = MDC.get(START_TIME);
    if (startTime != null) {
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - Long.parseLong(startTime);
      return String.valueOf(executionTime);
    }
    return "1";
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void restController() {
    // all rest controllers
  }

  @Pointcut("@within(org.springframework.stereotype.Repository)")
  public void repository() {
    // all repository methods
  }

  @Pointcut("@within(org.springframework.stereotype.Service)")
  public void service() {
    // all service methods
  }

  /** Log essential info of application during the startup. */
  @PostConstruct
  public void logStartup() {
    log.info("-> Starting {} version {} - environment {}", artifactId, version, environment);
  }

  /**
   * If DEBUG log-level is enabled prints the env variables and the application properties.
   *
   * @param event Context of application
   */
  @EventListener
  public void handleContextRefresh(ContextRefreshedEvent event) {
    final Environment env = event.getApplicationContext().getEnvironment();
    log.debug("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
    final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
    StreamSupport.stream(sources.spliterator(), false)
        .filter(EnumerablePropertySource.class::isInstance)
        .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
        .flatMap(Arrays::stream)
        .distinct()
        .filter(
            prop ->
                !(prop.toLowerCase().contains("credentials")
                    || prop.toLowerCase().contains("password")
                    || prop.toLowerCase().contains("pass")
                    || prop.toLowerCase().contains("pwd")
                    || prop.toLowerCase().contains("key")
                    || prop.toLowerCase().contains("secret")))
        .forEach(prop -> log.debug("{}: {}", prop, env.getProperty(prop)));
  }

  @Around(value = "restController()")
  public Object logApiInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    MDC.put(METHOD, joinPoint.getSignature().getName());
    MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
    log.info("{} {}", httRequest.getMethod(), httRequest.getRequestURI());
    log.info(
        "Invoking API operation {} - args: {}",
        joinPoint.getSignature().getName(),
        joinPoint.getArgs());

    Object result = joinPoint.proceed();

    MDC.put(STATUS, "OK");
    MDC.put(CODE, String.valueOf(httpResponse.getStatus()));
    MDC.put(RESPONSE_TIME, getExecutionTime());
    log.info(
        "Successful API operation {} - result: {}", joinPoint.getSignature().getName(), result);
    MDC.remove(STATUS);
    MDC.remove(CODE);
    MDC.remove(RESPONSE_TIME);
    MDC.remove(START_TIME);
    return result;
  }

  @AfterReturning(value = "execution(* *..exception.ErrorHandler.*(..))", returning = "result")
  public void trowingApiInvocation(JoinPoint joinPoint, ResponseEntity<?> result) {
    MDC.put(STATUS, "KO");
    MDC.put(CODE, String.valueOf(result.getStatusCodeValue()));
    MDC.put(RESPONSE_TIME, getExecutionTime());
    log.info("Failed API operation {} - error: {}", MDC.get(METHOD), result);
    MDC.remove(STATUS);
    MDC.remove(CODE);
    MDC.remove(RESPONSE_TIME);
    MDC.remove(START_TIME);
  }

  @Around(value = "repository() || service()")
  public Object logTrace(ProceedingJoinPoint joinPoint) throws Throwable {
    Set<String> methodNameToExclude = Set.of(
            "ConfigService.getCache()"
    );

    String methodName = joinPoint.getSignature().toShortString();

    log.debug("Call method {} - args: {}", methodName, joinPoint.getArgs());
    Object result = joinPoint.proceed();

    if (methodNameToExclude.contains(methodName)) {
      log.debug("Return method {} - result: {}", methodName, "skipped");
    }
    else {
      log.debug("Return method {} - result: {}", methodName, result);
    }

    return result;
  }
}
