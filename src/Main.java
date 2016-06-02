import java.io.File;
import java.io.IOException;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("\nEnter a directory path: ");

            String dirPath = scanner.nextLine();
            File dir = new File(dirPath);


            if (!dir.exists() || !dir.isDirectory()) {

                System.out.println("\nThe given path does not refer to a directory!");
            }
            else if (!dir.canRead()) {

                System.out.println("\nThe given directory is unreadable!");
            }
            else {

                System.out.print("Do you want to enable recursive searching [Y/n]? ");
                String argument = scanner.nextLine();
                boolean recursiveSearch = false;

                if (argument.isEmpty() || argument.toLowerCase().equals("y"))
                    recursiveSearch = true;

                DuplicateFinder duplicateFinder = new DuplicateFinder(recursiveSearch);

                duplicateFinder.findPossibleDuplicates(dir);
                duplicateFinder.checkOriginalFilesExisting();
                duplicateFinder.checkFilesEqual();
                duplicateFinder.processResults();
            }


            System.out.print("\nDo you want to continue [Y/n]? ");
            String argument = scanner.nextLine();

            if (!argument.isEmpty() && !argument.toLowerCase().equals("y"))
                break;
        }
    }
}
