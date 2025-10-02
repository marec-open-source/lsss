package no.imr.lsss.server.pojo;

public final class ErrorMessage {
   public String id;
   public String time;
   public String message;
   public String details;

   public ErrorMessage(String id, String time, String message, String details) {
      this.id = id;
      this.time = time;
      this.message = message;
      this.details = details;
   }

   @Override
   public String toString() {
      return "ErrorMessage{" +
            "id='" + id + '\'' +
            ", time='" + time + '\'' +
            ", message='" + message + '\'' +
            ", details='" + details + '\'' +
            '}';
   }
}
