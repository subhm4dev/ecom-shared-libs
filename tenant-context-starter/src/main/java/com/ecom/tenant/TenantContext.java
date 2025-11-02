package com.ecom.tenant;
import java.lang.ScopedValue;
import io.opentelemetry.api.baggage.*;
public final class TenantContext {
  private static final ScopedValue<String> TENANT = ScopedValue.newInstance();
  private static final ScopedValue<String> USER = ScopedValue.newInstance();
  public static void runWith(String tenant, String user, Runnable task) {
    ScopedValue.where(TENANT, tenant).where(USER, user).run(() -> {
      Baggage.current().toBuilder()
             .put("tenant.id", tenant)
             .put("user.id", user).build().makeCurrent();
      task.run();
    });
  }
  public static String tenant(){return TENANT.orElse(null);}
  public static String user(){return USER.orElse(null);}
}
