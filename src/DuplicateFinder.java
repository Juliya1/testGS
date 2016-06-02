import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.FileNotFoundException;

public class DuplicateFinder {

    private HashMap<File,ArrayList<File>> duplicates;

    private ArrayList<File> dirDuplicates;
    private ArrayList<File> fileDuplicates;

    private boolean recursiveSearch;


    public DuplicateFinder(boolean recursiveSearch) {

        this.duplicates = new HashMap<File, ArrayList<File>>();

        this.dirDuplicates = new ArrayList<File>();
        this.fileDuplicates = new ArrayList<File>();

        this.recursiveSearch = recursiveSearch;
    }

    public void findPossibleDuplicates(File dir) {

        File[] files = dir.listFiles();

        Pattern pattern = Pattern.compile("^(.+)(\\s\\(\\d+\\))(.*)$");

        for (int i = 0; i < files.length; i++) {

            File file = files[i];
            Matcher matcher = pattern.matcher(file.getName());

            if (!matcher.find()) {

                if (file.isDirectory() && recursiveSearch)
                    findPossibleDuplicates(file);

                continue;
            }

            String originalFileName = matcher.group(1) + matcher.group(3);
            File originalFile = new File(dir, originalFileName);

            if (!duplicates.containsKey(originalFile))
                duplicates.put(originalFile, new ArrayList<File>());

            duplicates.get(originalFile).add(file);
        }
    }

    public void checkOriginalFilesExisting() {

        ArrayList<File> nonExistingFiles = new ArrayList<File>();

        for (File originalFile : duplicates.keySet())
            if (!originalFile.exists())
                nonExistingFiles.add(originalFile);

        if (nonExistingFiles.isEmpty())
            return;

        for (File file : nonExistingFiles) {

            ArrayList<File> files = duplicates.get(file);

            if (files.size() > 1) {

                Collections.sort(files, new FileNameComparator());
                duplicates.put(files.remove(0), files);
            }

            duplicates.remove(file);
        }
    }

    public void checkFilesEqual() {

        for (File originalFile : duplicates.keySet()) {

            ArrayList<File> duplicateFiles = duplicates.get(originalFile);

            for (int i = duplicateFiles.size()-1; i >= 0; i--)
                if (!areFilesEqual(originalFile, duplicateFiles.get(i)))
                    duplicateFiles.remove(i);
        }
    }


    public void processResults() {

        System.out.println("\nThe file scanning is finished!");

        for (File originalFile : duplicates.keySet()) {

            if (duplicates.get(originalFile).size() == 0)
                continue;
            else if (originalFile.isFile())
                fileDuplicates.add(originalFile);
            else if (originalFile.isDirectory())
                dirDuplicates.add(originalFile);
        }

        if (dirDuplicates.isEmpty() && fileDuplicates.isEmpty()) {

            System.out.println("No duplicates were found!");
            return;
        }


        Scanner scanner = new Scanner(System.in);
        String argument;

        System.out.print("\nDo you want to output results to the screen [Y/n]? ");
        argument = scanner.nextLine();

        if (argument.isEmpty() || argument.toLowerCase().equals("y"))
            outputFoundDuplicates(null);

        while (true) {

            System.out.print("\nDo you want to save results to a file [Y/n]? ");
            argument = scanner.nextLine();

            if (!argument.isEmpty() && !argument.toLowerCase().equals("y"))
                break;

            System.out.print("\nEnter a file path: ");
            String path = scanner.nextLine();

            if (outputFoundDuplicates(path))
                break;
        }

        System.out.print("\nDo you want to remove found duplicates [y/N]? ");
        argument = scanner.nextLine();

        if (argument.isEmpty() || argument.toLowerCase().equals("n"))
            return;

        removeFoundDuplicates();
    }



    private boolean areFilesEqual(File originalFile, File duplicateFile) {

        if (originalFile.length() != duplicateFile.length())
            return false;

        if (originalFile.isDirectory()) {

            File[] originalContent = originalFile.listFiles();
            File[] duplicateContent = duplicateFile.listFiles();

            if (originalContent.length != duplicateContent.length)
                return false;

            Arrays.sort(originalContent, new FileNameComparator());
            Arrays.sort(duplicateContent, new FileNameComparator());

            for (int i = 0; i < originalContent.length; i++) {

                if (!originalContent[i].getName().equals(duplicateContent[i].getName()))
                    return false;
                else if (!areFilesEqual(originalContent[i], duplicateContent[i]))
                    return false;
            }
        }

        return true;
    }


    private boolean outputFoundDuplicates(String filePath) {

        PrintWriter out;

        try {

            if (filePath == null)
                out = new PrintWriter(System.out);
            else
                out = new PrintWriter(new File(filePath));
        }
        catch (FileNotFoundException e) {

            System.out.println("\nCannot write to the given file!");
            System.out.println("You may enter a new file path.");
            return false;
        }


        try {

            if (!dirDuplicates.isEmpty()) {

                Collections.sort(dirDuplicates, new FileNameComparator());

                out.println("\nDUPLICATES OF DIRECTORIES:");

                for (File originalFile : dirDuplicates) {

                    ArrayList<File> foundDuplicates = duplicates.get(originalFile);
                    Collections.sort(foundDuplicates, new FileNameComparator());

                    out.println("\n" + originalFile.getCanonicalPath() + ":");

                    for (File duplicateFile : foundDuplicates)
                        out.println("- " + duplicateFile.getCanonicalPath());
                }
            }

            if (!fileDuplicates.isEmpty()) {

                Collections.sort(fileDuplicates, new FileNameComparator());

                out.println("\nDUPLICATES OF FILES:");

                for (File originalFile : fileDuplicates) {

                    ArrayList<File> foundDuplicates = duplicates.get(originalFile);
                    Collections.sort(foundDuplicates, new FileNameComparator());

                    out.println("\n" + originalFile.getCanonicalPath() + ":");

                    for (File duplicateFile : foundDuplicates)
                        out.println("- " + duplicateFile.getCanonicalPath());
                }
            }
        }
        catch (IOException e) {

            System.out.println(e.toString());
            return false;
        }
        finally {

            out.flush();

            if (filePath != null)
                out.close();
        }

        return true;
    }


    private void removeFoundDuplicates() {
        if (!dirDuplicates.isEmpty()) {

            for (File originalDir : dirDuplicates) {

                for (File duplicateDir : duplicates.get(originalDir)) {

                    try {

                        String dirPath = duplicateDir.getCanonicalPath();

                        if (removeDirectory(duplicateDir))
                            System.out.println(dirPath + " is removed!");
                        else
                            System.out.println("Cannot remove " + dirPath + "!");
                    }
                    catch (IOException e) {

                        System.out.println(e.toString());
                        continue;
                    }
                }
            }
        }

        if (!fileDuplicates.isEmpty()) {

            for (File originalFile : fileDuplicates) {

                for (File duplicateFile : duplicates.get(originalFile)) {

                    try {

                        String filePath = duplicateFile.getCanonicalPath();

                        if (duplicateFile.delete())
                            System.out.println(filePath + " is removed!");
                        else
                            System.out.println("Cannot remove " + filePath + "!");
                    }
                    catch (IOException e) {

                        System.out.println(e.toString());
                        continue;
                    }
                }
            }
        }
    }

    private boolean removeDirectory(File dir) {

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {

            if(files[i].isDirectory())
                removeDirectory(files[i]);
            else
                files[i].delete();
        }

        return dir.delete();
    }
}