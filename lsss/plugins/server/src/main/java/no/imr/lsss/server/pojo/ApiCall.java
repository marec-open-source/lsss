package no.imr.lsss.server.pojo;

public final class ApiCall {
   public int number;
   public long time = System.currentTimeMillis();
   public String method;
   public String url;
   public String requestBody = "";
   public int status;

   public ApiCall(int number, String method, String url) {
      this.number = number;
      this.method = method;
      this.url = url;
   }

   @Override
   public String toString() {
      return "ApiCall{" +
            "number=" + number +
            ", time=" + time +
            ", method='" + method + '\'' +
            ", url='" + url + '\'' +
            ", requestBody='" + requestBody + '\'' +
            ", status=" + status +
            '}';
   }
}
