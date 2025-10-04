package jayslabs.kafka.section11.dto;

public record Phone(int phoneNumber) implements ContactMethod {

    @Override
    public void contact() {
        System.out.println("Calling phone number " + phoneNumber);
    }

}
