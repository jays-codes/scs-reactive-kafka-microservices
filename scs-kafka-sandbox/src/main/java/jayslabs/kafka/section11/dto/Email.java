package jayslabs.kafka.section11.dto;

public record Email(String email) implements ContactMethod {

    @Override
    public void contact() {
        System.out.println("Sending email to " + email);
    }

}


