import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class HelpDesk {

    enum Priority {
        LOW, MEDIUM, HIGH
    }

    enum Status {
        OPEN, IN_PROGRESS, RESOLVED
    }

    static class Ticket {
        private final int id;
        private final String studentName;
        private final String department;
        private final String location;
        private final String issue;
        private final Priority priority;

        private String technician;
        private Status status;

        Ticket(
            int id,
            String studentName,
            String department,
            String location,
            String issue,
            Priority priority,
            String technician,
            Status status
        ) {
            this.id = id;
            this.studentName = studentName;
            this.department = department;
            this.location = location;
            this.issue = issue;
            this.priority = priority;
            this.technician = technician;
            this.status = status;
        }

        void display() {
            System.out.println("\n--------------------------------");
            System.out.println("Request ID : " + id);
            System.out.println("Student    : " + studentName);
            System.out.println("Department : " + department);
            System.out.println("Location   : " + location);
            System.out.println("Problem    : " + issue);
            System.out.println("Priority   : " + priority);
            System.out.println("Technician : " + technician);
            System.out.println("Status     : " + status);
        }

        String toFileLine() {
            return id + "\t"
                + studentName + "\t"
                + department + "\t"
                + location + "\t"
                + issue + "\t"
                + priority + "\t"
                + technician + "\t"
                + status;
        }
    }

    private static final Scanner input = new Scanner(System.in);
    private static final List<Ticket> tickets = new ArrayList<>();

    private static final Path DATA_FILE =
        Path.of("college_requests.tsv");

    private static int nextId = 1;

    public static void main(String[] args) {
        try {
            loadTickets();
        } catch (IOException e) {
            System.out.println("Cannot load requests: " + e.getMessage());
            System.out.println(
                "Check college_requests.tsv before running again."
            );
            input.close();
            return;
        }

        boolean running = true;

        while (running) {
            System.out.println(
                "\n====== COLLEGE IT SUPPORT SYSTEM ======"
            );
            System.out.println("1. Report a new problem");
            System.out.println("2. View all support requests");
            System.out.println("3. Assign technician / Update status");
            System.out.println("4. Search support requests");
            System.out.println("5. View summary");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            String choice = input.nextLine().trim();

            switch (choice) {
                case "1":
                    createRequest();
                    break;

                case "2":
                    viewRequests();
                    break;

                case "3":
                    updateRequest();
                    break;

                case "4":
                    searchRequests();
                    break;

                case "5":
                    showSummary();
                    break;

                case "6":
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Enter a choice from 1 to 6.");
            }
        }

        input.close();
    }

    private static String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim();

            if (value.isEmpty()) {
                System.out.println("This field cannot be empty.");
            } else if (value.contains("\t")) {
                System.out.println("Please do not use tab characters.");
            } else {
                return value;
            }
        }
    }

    private static void createRequest() {
        System.out.println("\nREPORT A NEW PROBLEM");

        String name = readRequired("Student name: ");
        String department = readRequired("Department: ");
        String location = readRequired("Location / Lab / Room: ");
        String issue = readRequired("Describe the problem: ");

        Priority priority;

        while (true) {
            String text = readRequired(
                "Priority (LOW / MEDIUM / HIGH): "
            );

            try {
                priority = Priority.valueOf(
                    text.toUpperCase(Locale.ROOT)
                );
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Choose LOW, MEDIUM or HIGH.");
            }
        }

        if (nextId == Integer.MAX_VALUE) {
            System.out.println("No more request IDs are available.");
            return;
        }

        Ticket ticket = new Ticket(
            nextId,
            name,
            department,
            location,
            issue,
            priority,
            "UNASSIGNED",
            Status.OPEN
        );

        tickets.add(ticket);

        try {
            saveTickets();
            nextId++;

            System.out.println("\nSupport request recorded and saved!");
            ticket.display();

        } catch (IOException e) {
            tickets.remove(ticket);
            System.out.println(
                "Request could not be saved: " + e.getMessage()
            );
        }
    }

    private static void viewRequests() {
        if (tickets.isEmpty()) {
            System.out.println("No support requests yet.");
            return;
        }

        for (Ticket ticket : tickets) {
            ticket.display();
        }
    }

    private static Ticket findTicket(int id) {
        for (Ticket ticket : tickets) {
            if (ticket.id == id) {
                return ticket;
            }
        }

        return null;
    }

    private static void updateRequest() {
        if (tickets.isEmpty()) {
            System.out.println("No support requests to update.");
            return;
        }

        System.out.print("Enter request ID: ");
        int id;

        try {
            id = Integer.parseInt(input.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Request ID must be a number.");
            return;
        }

        Ticket ticket = findTicket(id);

        if (ticket == null) {
            System.out.println("Request ID not found.");
            return;
        }

        ticket.display();

        String technician = readRequired(
            "Technician name (or UNASSIGNED): "
        );

        Status newStatus;

        while (true) {
            String text = readRequired(
                "Status (OPEN / IN_PROGRESS / RESOLVED): "
            );

            try {
                newStatus = Status.valueOf(
                    text.toUpperCase(Locale.ROOT)
                );
                break;
            } catch (IllegalArgumentException e) {
                System.out.println(
                    "Choose OPEN, IN_PROGRESS or RESOLVED."
                );
            }
        }

        if (
            !newStatus.equals(Status.OPEN)
            && technician.equalsIgnoreCase("UNASSIGNED")
        ) {
            System.out.println(
                "Assign a technician before marking work "
                + "IN_PROGRESS or RESOLVED."
            );
            return;
        }

        String previousTechnician = ticket.technician;
        Status previousStatus = ticket.status;

        ticket.technician = technician;
        ticket.status = newStatus;

        try {
            saveTickets();
            System.out.println("\nRequest updated and saved!");
            ticket.display();

        } catch (IOException e) {
            ticket.technician = previousTechnician;
            ticket.status = previousStatus;

            System.out.println(
                "Update could not be saved: " + e.getMessage()
            );
        }
    }

    private static void searchRequests() {
        String query = readRequired(
            "Search by student, department, location or problem: "
        ).toLowerCase(Locale.ROOT);

        boolean found = false;

        for (Ticket ticket : tickets) {
            String searchableText =
                ticket.studentName + " "
                + ticket.department + " "
                + ticket.location + " "
                + ticket.issue;

            if (
                searchableText.toLowerCase(Locale.ROOT)
                    .contains(query)
            ) {
                ticket.display();
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching support requests.");
        }
    }

    private static void showSummary() {
        int open = 0;
        int inProgress = 0;
        int resolved = 0;

        for (Ticket ticket : tickets) {
            switch (ticket.status) {
                case OPEN:
                    open++;
                    break;

                case IN_PROGRESS:
                    inProgress++;
                    break;

                case RESOLVED:
                    resolved++;
                    break;
            }
        }

        System.out.println("\nSUPPORT SUMMARY");
        System.out.println("Total requests : " + tickets.size());
        System.out.println("OPEN           : " + open);
        System.out.println("IN_PROGRESS    : " + inProgress);
        System.out.println("RESOLVED       : " + resolved);
    }

    private static void saveTickets() throws IOException {
        List<String> lines = new ArrayList<>();

        for (Ticket ticket : tickets) {
            lines.add(ticket.toFileLine());
        }

        Path target = DATA_FILE.toAbsolutePath();
        Path temporaryFile = Files.createTempFile(
            target.getParent(),
            "college-support-",
            ".tmp"
        );

        try {
            Files.write(
                temporaryFile,
                lines,
                StandardCharsets.UTF_8
            );

            Files.move(
                temporaryFile,
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void loadTickets() throws IOException {
        if (!Files.exists(DATA_FILE)) {
            return;
        }

        List<String> lines = Files.readAllLines(
            DATA_FILE,
            StandardCharsets.UTF_8
        );

        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;

            try {
                String[] parts = line.split("\t", -1);

                if (parts.length != 8) {
                    throw new IllegalArgumentException();
                }

                int id = Integer.parseInt(parts[0]);

                if (
                    id < 1
                    || id == Integer.MAX_VALUE
                    || findTicket(id) != null
                ) {
                    throw new IllegalArgumentException();
                }

                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].trim().isEmpty()) {
                        throw new IllegalArgumentException();
                    }
                }

                Priority priority = Priority.valueOf(parts[5]);
                Status status = Status.valueOf(parts[7]);

                if (
                    !status.equals(Status.OPEN)
                    && parts[6].equalsIgnoreCase("UNASSIGNED")
                ) {
                    throw new IllegalArgumentException();
                }

                tickets.add(new Ticket(
                    id,
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    priority,
                    parts[6],
                    status
                ));

                nextId = Math.max(nextId, id + 1);

            } catch (IllegalArgumentException e) {
                throw new IOException(
                    "Invalid request data at line " + lineNumber,
                    e
                );
            }
        }
    }
}