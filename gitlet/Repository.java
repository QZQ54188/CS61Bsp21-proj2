package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.StagingArea.getBlobByID;
import static gitlet.Utils.*;


// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    private static final String DEFAULT_BRANCH_NAME = "master";

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECT_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");

    public static final File ADDSTAGE_FILE = join(GITLET_DIR, "add_stage");
    public static final File REMOVESTAGE_FILE = join(GITLET_DIR, "remove_stage");

    public static Commit curCommit;

    public static StagingArea addStage = new StagingArea();
    public static StagingArea removeStage = new StagingArea();
    public static String curBranch;


    /**
     * Initialize a repository at the current working directory.
     *
     * <pre>
     * .gitlet
     * ├── HEAD
     * ├── objects
     * └── refs
     *     └── heads
     * </pre>
     */
    public static void init(){
        if(GITLET_DIR.exists()){
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        mkdir(GITLET_DIR);
        mkdir(OBJECT_DIR);
        mkdir(REFS_DIR);
        mkdir(HEADS_DIR);
        writeContents(HEAD_FILE, "master");
        initCommit();
        initHeads();
    }

    private static void initCommit(){
        Commit init = new Commit();
        curCommit = init;
        init.save();
    }

    private static void initHeads(){
        File headsFile = join(HEADS_DIR, "master");
        writeContents(headsFile, curCommit.getCommitHash());
    }

    /**Check whether the warehouse has been initialized when using the add method*/
    public static void checkInitialized(){
        if(!GITLET_DIR.exists()){
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /**Add a file named file to the staging area. During this process,
     * we need to calculate the BlobID of the file and detect whether
     * a file with the same content as the file to be added
     * already exists in the staging area.*/
    public static void add(String fileName){
        File file = getFileFromCWD(fileName);
        if(!file.exists()){
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Blob blob = new Blob(file);
        //Store blob files in the temporary storage area
        curCommit = readCurCommit();
        addStage = readAddStage();
        removeStage = readRemoveStage();
        if(!curCommit.getPathToBlobID().containsValue(blob.getBlobID())||
                !removeStage.isNewBlob(blob)){
            if(addStage.isNewBlob(blob)){
                if(removeStage.isNewBlob(blob)){
                    blob.save();
                    if(addStage.isFilePathExists(blob.getFilePath())){
                        addStage.delete(blob);
                    }
                    addStage.add(blob);
                    addStage.saveAddStage();
                }else{
                    removeStage.delete(blob);
                    removeStage.saveRemoveStage();
                }
            }
        }
    }

    private static File getFileFromCWD(String fileName){
        return Paths.get(fileName).isAbsolute() ? new File(fileName) : join(CWD, fileName);
    }

    private static Commit readCurCommit(){
        String curCommitID = readCurCommitID();
        File curCommitFile = join(OBJECT_DIR, curCommitID);
        return readObject(curCommitFile, Commit.class);
    }

    private static String readCurCommitID(){
        String curBranch = readCurBranch();
        File headsFile = join(HEADS_DIR, curBranch);
        return readContentsAsString(headsFile);
    }

    private static String readCurBranch(){
        return readContentsAsString(HEAD_FILE);
    }

    private static StagingArea readAddStage(){
        if(!ADDSTAGE_FILE.exists()){
            return new StagingArea();
        }
        return readObject(ADDSTAGE_FILE, StagingArea.class);
    }

    private static StagingArea readRemoveStage(){
        if(!REMOVESTAGE_FILE.exists()){
            return new StagingArea();
        }
        return readObject(REMOVESTAGE_FILE, StagingArea.class);
    }

    /**commit command function*/
    public static void commit(String message){
        if(message.isEmpty()){
            System.out.println("Please enter a commit message");
            System.exit(0);
        }
        //Create a commit based on the message and save it
        Commit newCommit = newCommit(message);
        saveNewCommit(newCommit);
    }

    private static Commit newCommit(String message){
        Map<String, String> addBlob = findAddBlob();
        Map<String, String> removeBlob = findRemoveBlob();
        checkIfNewCommit(addBlob, removeBlob);

        curCommit = readCurCommit();
        Map<String,String> blobMap = getBlobMapFromCurCommit(curCommit);

        blobMap = caculateBlobMap(blobMap, addBlob, removeBlob);
        List<String> parents = findParents();
        return new Commit(message, blobMap, parents);
    }

    private static void saveNewCommit(Commit newCommit){
        newCommit.save();
        addStage.clear();
        addStage.saveAddStage();
        removeStage.clear();
        removeStage.saveRemoveStage();
        curCommit = newCommit;
        //Replace the contents of the refs/heads file with the hash value of the latest commit
        String curBranch = readCurBranch();
        File headsFile = join(HEADS_DIR, curBranch);
        writeContents(headsFile, curCommit.getCommitHash());
    }

    private static Map<String, String> findAddBlob(){
        Map<String, String> addBlob = new HashMap<>();
        addStage = readAddStage();
        List<Blob> addBlobList = addStage.getBlobList();
        for(Blob blob : addBlobList){
            addBlob.put(blob.getFilePath(), blob.getBlobID());
        }
        return addBlob;
    }

    private static Map<String, String> findRemoveBlob(){
        Map<String, String> removeBlob = new HashMap<>();
        removeStage = readRemoveStage();
        List<Blob> removeBlobList = removeStage.getBlobList();
        for(Blob blob : removeBlobList){
            removeBlob.put(blob.getFilePath(), blob.getBlobID());
        }
        return removeBlob;
    }

    private static void checkIfNewCommit(Map<String, String> addBlob,
                                         Map<String, String> removeBlob){
        if(addBlob.isEmpty() && removeBlob.isEmpty()){
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
    }

    private static Map<String, String> getBlobMapFromCurCommit(Commit curCommit){
        return curCommit.getPathToBlobID();
    }

    private static Map<String, String> caculateBlobMap(Map<String, String> blobMap,
                                                       Map<String, String> addBlob,
                                                       Map<String, String> removeBlob){
        if(!addBlob.isEmpty()){
            for(String path : addBlob.keySet()){
                blobMap.put(path, addBlob.get(path));
            }
        }

        if(!removeBlob.isEmpty()){
            for(String path : removeBlob.keySet()){
                blobMap.remove(path);
            }
        }
        return blobMap;
    }

    private static List<String> findParents(){
        List<String> parents = new ArrayList<>();
        curCommit = readCurCommit();
        parents.add(curCommit.getCommitHash());
        return parents;
    }



    /**rm command function
     * If the file is in the staging area, remove it from the staging area.
     * If the file is in the current commit, add it to the removal stage.
     * If the file is neither in the staging area nor in the current commit,
     * print a message and terminate the program.*/
    public static void rm(String fileName){
        File file = getFileFromCWD(fileName);
        String filePath = file.getPath();

        addStage = readAddStage();
        curCommit = readCurCommit();

        if(addStage.exists(filePath)){
            addStage.delete(filePath);
            addStage.saveAddStage();
        }else if(curCommit.exists(filePath)){
            removeStage = readRemoveStage();
            Blob blobToRemove = getBlobFromCurCommitByPath(filePath, curCommit);
            removeStage.add(blobToRemove);
            removeStage.saveRemoveStage();
            deleteFile(file);
        }else{
            System.out.println("No reason to remove the file");
            System.exit(0);
        }
    }

    private static Blob getBlobFromCurCommitByPath(String filePath, Commit curCommit){
        String blobID = curCommit.getPathToBlobID().get(filePath);
        return getBlobByID(blobID);
    }

    /**The deleteFile method is used to delete a file, but only if
     * the file is not a directory. It decides whether to delete the
     * file by checking whether it exists and whether it is a normal file
     * (rather than a directory).*/
    private static boolean deleteFile(File file){
        if(file.exists() && !file.isDirectory()){
            return file.delete();
        }
        return false;
    }



    /**log command function*/
    public static void log(){
        curCommit = readCurCommit();
        while (!curCommit.getParents().isEmpty()){
            if(isMergeCommit(curCommit)){
                printMergeCommit(curCommit);
            }else{
                printCommit(curCommit);
            }
            List<String> parents = curCommit.getParents();
            curCommit = readCommitByID(parents.get(0));
        }
        printCommit(curCommit);
    }

    private static boolean isMergeCommit(Commit curCommit){
        return curCommit.getParents().size() == 2;
    }

    private static void printCommit(Commit curCommit){
        System.out.println("===");
        printCommitID(curCommit);
        printCommitDate(curCommit);
        printCommitMessage(curCommit);
    }

    private static void printMergeCommit(Commit curCommit){
        System.out.println("===");
        printCommitID(curCommit);
        printMergeBranch(curCommit);
        printCommitDate(curCommit);
        printCommitMessage(curCommit);
    }

    private static Commit readCommitByID(String id){
        if(id.length() == 40){
            File curCommitFile = join(OBJECT_DIR, id);
            if(!curCommitFile.exists()){
                return null;
            }
            return readObject(curCommitFile, Commit.class);
        }else{
            List<String> objectID = plainFilenamesIn(OBJECT_DIR);
            for(String fileName : objectID){
                if(fileName.substring(0,id.length()).equals(id)){
                    return readObject(join(OBJECT_DIR, fileName), Commit.class);
                }
            }
            return null;
        }
    }

    private static void printCommitID(Commit curCommit){
        System.out.println("commit: " + curCommit.getCommitHash());
    }

    private static void printCommitDate(Commit curCommit){
        System.out.println("Date: " + curCommit.getTimeStamp());
    }

    private static void printCommitMessage(Commit curCommit){
        System.out.println(curCommit.getMessage() + "\n");
    }

    private static void printMergeBranch(Commit curCommit){
        List<String> parents = curCommit.getParents();
        String parent1 = parents.get(0);
        String parent2 = parents.get(1);
        System.out.println("Merge: " + parent1.substring(0, 7) + parent2.substring(0, 7));
    }



    /**global-log command function*/
    public static void global_log() {
        List<String> commitList = plainFilenamesIn(OBJECT_DIR);
        for (String id : commitList) {
            try {
                if (isCommitObject(id)) {
                    Commit commit = readCommitByID(id);
                    if (commit != null) {
                        if (isMergeCommit(commit)) {
                            printMergeCommit(commit);
                        } else {
                            printCommit(commit);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing commit ID: " + id + " - " + e.getMessage());
            }
        }
    }

    private static boolean isCommitObject(String id) {
        File file = join(OBJECT_DIR, id);
        // Try reading the file as a commit object
        try {
            Commit commit = readObject(file, Commit.class);
            return commit != null;
        } catch (Exception e) {
            return false;
        }
    }



    /**Print out the IDs of all commits with a given commit message, one per line*/
    public static void find(String findMessage){
        List<String> commitList = plainFilenamesIn(OBJECT_DIR);
        List<String> idList = new ArrayList<>();
        for(String id : commitList){
            try{
                if(isCommitObject(id)){
                    Commit commit = readCommitByID(id);
                    if(commit != null && findMessage.equals(commit.getMessage())){
                        idList.add(id);
                    }
                }
            }catch (Exception ignore){
                System.err.println("Error processing commit ID: " + id + "-" + ignore.getMessage());
            }
        }
        printID(idList);
    }


    private static void printID(List<String> idList){
        if(idList.isEmpty()){
            System.out.println("Found no commit with that message.");
        }else{
            for(String id : idList){
                System.out.println(id);
            }
        }
    }



    /**status command function*/
    public static void status(){
        printBranches();
        printStagedFiles();
        printRemovedFiles();
        printModifiedNotStagedFiles();
        printUntrackedFiles();
    }

    private static void printBranches(){
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        curBranch = readCurBranch();
        System.out.println("=== Branches ===");
        System.out.println("*" + curBranch);
        //Indicates that there is more than the default branch
        if(branchNames.size() > 1){
            for(String branch : branchNames){
                if(!branch.equals(curBranch)){
                    System.out.println(branch);
                }
            }
        }
        System.out.println();
    }

    private static void printStagedFiles(){
        System.out.println("=== Staged Files ===");
        addStage = readAddStage();
        for(Blob blob : addStage.getBlobList()){
            System.out.println(blob.getFileName());
        }
        System.out.println();
    }

    private static void printRemovedFiles(){
        System.out.println("=== Removed Files ===");
        removeStage = readRemoveStage();
        for(Blob blob : removeStage.getBlobList()){
            System.out.println(blob.getFileName());
        }
        System.out.println();
    }

    /**These two methods will be added later when reviewing the code.*/
    private static void printModifiedNotStagedFiles(){
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
    }

    private static void printUntrackedFiles(){
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }



    /**checkout command functions
     * case 1: java gitlet.Main checkout -- [file name]*/
    public static void checkout(String fileName){
        Commit curCommit = readCurCommit();
        List<String> fileNames = curCommit.getFileNames();
        if(fileNames.contains(fileName)){
            Blob blob = curCommit.getBlobByFileName(fileName);
            writeBlobToCWD(blob);
        }else{
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    private static void writeBlobToCWD(Blob blob){
        File file = join(CWD, blob.getFileName());
        byte[] contents = blob.getContents();
        writeContents(file, new String(contents, StandardCharsets.UTF_8));
    }


    /**case 2: java gitlet.Main checkout [commit id] -- [file name]*/
    public static void checkout(String commitID, String fileName){
        Commit commit = readCommitByID(commitID);
        if(commit == null){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        List<String> fileNames = commit.getFileNames();
        if(fileNames.contains(fileName)){
            Blob blob = commit.getBlobByFileName(fileName);
            writeBlobToCWD(blob);
        }else{
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }


    /**case 3: java gitlet.Main checkout [branch name]*/
    public static void checkoutBranch(String branchName){
        List<String> allBranches = plainFilenamesIn(HEADS_DIR);
        if(!allBranches.contains(branchName)){
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        curBranch = readCurBranch();
        if(branchName.equals(curBranch)){
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        Commit newCommit = readCommitByBranchName(branchName);
        changeCurCommitTo(newCommit);

        writeContents(HEAD_FILE, branchName);
    }

    private static Commit readCommitByBranchName(String branchName){
        File branchFile = join(HEADS_DIR, branchName);
        String newCommitID = readContentsAsString(branchFile);
        return readCommitByID(newCommitID);
    }

    private static void changeCurCommitTo(Commit newCommit){
        List<String> onlyCurCommitTracked = findOnlyCurCommitTracked(newCommit);
        List<String> bothCommitTracked = findBothCommitTracked(newCommit);
        List<String> onlyNewCommitTracked = findOnlyNewCommitTracked(newCommit);
        deleteFiles(onlyCurCommitTracked);
        overWriteFiles(bothCommitTracked, newCommit);
        writeFiles(onlyNewCommitTracked, newCommit);
        clearAllStage();
    }

    private static List<String> findOnlyCurCommitTracked(Commit newCommit){
        List<String> onlyCurCommitTracked = curCommit.getFileNames();
        List<String> newCommitFiles = newCommit.getFileNames();
        for(String fileName : newCommitFiles){
            onlyCurCommitTracked.remove(fileName);
        }
        return onlyCurCommitTracked;
    }

    private static List<String> findBothCommitTracked(Commit newCommit){
        List<String> curCommitTracked = curCommit.getFileNames();
        List<String> newCommitFiles = newCommit.getFileNames();
        List<String> bothCommitTracked = new ArrayList<>();
        for(String fileName : newCommitFiles){
            if(curCommitTracked.contains(fileName)){
                bothCommitTracked.add(fileName);
            }
        }
        return bothCommitTracked;
    }

    private static List<String> findOnlyNewCommitTracked(Commit newCommit){
        List<String> onlyNewCommitTracked = newCommit.getFileNames();
        List<String> curCommitFiles = curCommit.getFileNames();
        for(String fileName : curCommitFiles){
            onlyNewCommitTracked.remove(fileName);
        }
        return onlyNewCommitTracked;
    }

    private static void deleteFiles(List<String> onlyCurCommitTracked){
        if(onlyCurCommitTracked.isEmpty()){
            return;
        }
        for(String fileName : onlyCurCommitTracked){
            File file = join(CWD, fileName);
            restrictedDelete(file);
        }
    }

    private static void overWriteFiles(List<String> bothCommitTracked, Commit newCommit){
        if(bothCommitTracked.isEmpty()){
            return;
        }
        for(String fileName : bothCommitTracked){
            Blob blob = newCommit.getBlobByFileName(fileName);
            writeBlobToCWD(blob);
        }
    }

    private static void writeFiles(List<String> onlyNewCommitTracked, Commit newCommit){
        if(onlyNewCommitTracked.isEmpty()){
            return;
        }
        for(String fileName : onlyNewCommitTracked){
            File file = join(CWD, fileName);
            if(file.exists()){
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        overWriteFiles(onlyNewCommitTracked, newCommit);
    }

    private static void clearAllStage(){
        addStage =readAddStage();
        removeStage=readRemoveStage();
        addStage.clear();
        removeStage.clear();
        addStage.saveAddStage();
        removeStage.saveRemoveStage();
    }


    /**branch command function*/
    public static void branch(String branchName){
        List<String> allBranches = plainFilenamesIn(HEADS_DIR);
        if(allBranches.contains(branchName)){
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        File newBranchFile = join(HEADS_DIR, branchName);
        curCommit = readCurCommit();
        writeContents(newBranchFile, curCommit.getCommitHash());
    }



    /**rm-branch command function*/
    public static void rm_branch(String branchName){
        curBranch = readCurBranch();
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if(curBranch.equals(branchName)){
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        if(!branches.contains(branchName)){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        File file = join(HEADS_DIR, branchName);
        if(!file.isDirectory()){
            file.delete();
        }
    }



    /**reset command function*/
    public static void reset(String commitID ){
        Commit newCommit = readCommitByID(commitID);
        if(newCommit == null){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        curCommit = readCurCommit();
        changeCurCommitTo(newCommit);

        curBranch = readCurBranch();
        File branchFile = join(HEADS_DIR, curBranch);
        writeContents(branchFile, commitID);
    }



    /**merge command function*/
    public static void merge(String branchName){
        curBranch = readCurBranch();

        addStage = readAddStage();
        removeStage = readRemoveStage();
        if(!addStage.isEmpty() && !removeStage.isEmpty()){
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if(!branches.contains(branchName)){
            System.out.println("A branch with that name does not exist. ");
            System.exit(0);
        }

        if(curBranch.equals(branchName)){
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        curCommit = readCurCommit();
        Commit mergeCommit = readCommitByBranchName(branchName);
        Commit splitPoint = findSplitPoint(curCommit, mergeCommit);

        if(splitPoint.getCommitHash().equals(mergeCommit.getCommitHash())){
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        if(splitPoint.getCommitHash().equals(curCommit.getCommitHash())){
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
        }

        Map<String, String> curCommitBlobs = curCommit.getPathToBlobID();

        String message = "Merged" + branchName + "into" + curBranch + ".";
        String curBranchCommitID = readCommitByBranchName(curBranch).getCommitHash();
        String mergeBranchCommitID = readCommitByBranchName(branchName).getCommitHash();

        List<String> parents = new ArrayList<>(Arrays.asList(curBranchCommitID, mergeBranchCommitID));
        Commit newCommit = new Commit(message, curCommitBlobs, parents);

        Commit mergedCommit = mergeFilesToNewCommit(splitPoint, newCommit, mergeCommit);
    }

    private static Commit findSplitPoint(Commit curCommit, Commit mergeCommit){
        Map<String, Integer> curCommitIDToLength = caculateCommitMap(curCommit, 0);
        Map<String, Integer> mergeCommitIDToLength = caculateCommitMap(mergeCommit, 0);
        return caculateSplitPoint(curCommitIDToLength, mergeCommitIDToLength);
    }

    private static Map<String, Integer> caculateCommitMap(Commit commit, int length){
        Map<String, Integer> map = new HashMap<>();
        if(commit.getParents().isEmpty()){
            map.put(commit.getCommitHash(), length);
            return map;
        }
        map.put(commit.getCommitHash(), length++);
        for(String id : commit.getParents()){
            Commit parent = readCommitByID(id);
            if (parent != null) {
                map.putAll(caculateCommitMap(parent, length));
            }
        }
        return map;
    }

    private static Commit caculateSplitPoint(Map<String, Integer> map1,
                                             Map<String, Integer> map2){
        int minLength = Integer.MAX_VALUE;
        String minID = "";
        for(String id : map1.keySet()){
            if(map2.containsKey(id) && map2.get(id) < minLength){
                minLength = map2.get(id);
                minID = id;
            }
        }
        return readCommitByID(minID);
    }


    private static Commit mergeFilesToNewCommit(Commit splitPoint, Commit newCommit, Commit mergeCommit){
        List<String> allFiles = caculateAllFiles(splitPoint, newCommit, mergeCommit);

        /*
         * case 1 5 6: write mergeCommit files into newCommit
         * case 1: overwrite files
         * case 5: write files
         * case 6: delete files
         */

        List<String> overWriteFiles = caculateOverWriteFiles(splitPoint, newCommit, mergeCommit);
        List<String> writeFiles = caculateWriteFiles(splitPoint, newCommit, mergeCommit);
        List<String> deleteFiles = caculateDeleteFiles(splitPoint, newCommit, mergeCommit);

        overWriteFiles(changeBlobIDListToFileNameList(overWriteFiles), mergeCommit);
        writeFiles(changeBlobIDListToFileNameList(writeFiles), mergeCommit);
        deleteFiles(changeBlobIDListToFileNameList(deleteFiles));

        /**case 3-1 : deal conflict*/
        checkIfConflict(allFiles, splitPoint, newCommit, mergeCommit);

        return caculateMergedCommit(newCommit, overWriteFiles, writeFiles, deleteFiles);

    }

    private static List<String> caculateAllFiles(Commit splitPoint, Commit newCommit, Commit mergeCommit){
        List<String> allFiles = new ArrayList<>(splitPoint.getBlobIDList());
        allFiles.addAll(newCommit.getBlobIDList());
        allFiles.addAll(mergeCommit.getBlobIDList());
        Set<String> set = new HashSet<>(allFiles);
        allFiles.clear();
        allFiles.addAll(set);
        return allFiles;
    }

    private static List<String> caculateOverWriteFiles(Commit splitPoint, Commit newCommit, Commit mergeCommit){
        Map<String, String> splitPointMap = splitPoint.getPathToBlobID();
        Map<String, String> newCommitMap = newCommit.getPathToBlobID();
        Map<String, String> mergeCommitMap = mergeCommit.getPathToBlobID();
        List<String> overWriteFiles = new ArrayList<>();

        for(String path : splitPointMap.keySet()){
            if(newCommitMap.containsKey(path) && mergeCommitMap.containsKey(path)){
                if(splitPointMap.get(path).equals(newCommitMap.get(path))&&
                !splitPointMap.get(path).equals(mergeCommitMap.get(path))){
                    overWriteFiles.add(mergeCommitMap.get(path));
                }
            }
        }
        return overWriteFiles;
    }

    private static List<String> caculateWriteFiles(Commit splitPoint, Commit newCommit, Commit mergeCommit){
        Map<String, String> splitPointMap = splitPoint.getPathToBlobID();
        Map<String, String> newCommitMap = newCommit.getPathToBlobID();
        Map<String, String> mergeCommitMap = mergeCommit.getPathToBlobID();
        List<String> writeFiles = new ArrayList<>();

        for(String path : mergeCommitMap.keySet()){
            if(!splitPointMap.containsKey(path) && !newCommitMap.containsKey(path)){
                writeFiles.add(mergeCommitMap.get(path));
            }
        }
        return writeFiles;
    }

    private static List<String> caculateDeleteFiles(Commit splitPoint, Commit
            newCommit, Commit mergeCommit){
        Map<String, String> splitPointMap = splitPoint.getPathToBlobID();
        Map<String, String> newCommitMap = newCommit.getPathToBlobID();
        Map<String, String> mergeCommitMap = mergeCommit.getPathToBlobID();
        List<String> deleteFiles = new ArrayList<>();

        for(String path : splitPointMap.keySet()){
            if(newCommitMap.containsKey(path) && !mergeCommitMap.containsKey(path)){
                deleteFiles.add(newCommitMap.get(path));
            }
        }
        return deleteFiles;
    }

    private static List<String> changeBlobIDListToFileNameList(List<String> blobIDList){
        List<String> fileNameList = new ArrayList<>();
        for(String id : blobIDList){
            Blob blob = getBlobByID(id);
            fileNameList.add(blob.getFileName());
        }
        return fileNameList;
    }

    private static void checkIfConflict(List<String> allFiles, Commit splitPoint, Commit newCommit, Commit mergeCommit){
        Map<String, String> splitPointMap = splitPoint.getPathToBlobID();
        Map<String, String> newCommitMap = newCommit.getPathToBlobID();
        Map<String, String> mergeCommitMap = mergeCommit.getPathToBlobID();

        boolean conflict = false;

        for(String blobID : allFiles){
            String path = getBlobByID(blobID).getFilePath();
            int commonPath = 0;
            if (splitPointMap.containsKey(path)) {
                commonPath += 1;
            }
            if (newCommitMap.containsKey(path)) {
                commonPath += 2;
            }
            if (mergeCommitMap.containsKey(path)) {
                commonPath += 4;
            }
            if ((commonPath == 3 && (!splitPointMap.get(path).equals(newCommitMap.get(path)))) ||
                    (commonPath == 5 && (!splitPointMap.get(path).equals(mergeCommitMap.get(path)))) ||
                    (commonPath == 6 && (!newCommitMap.get(path).equals(mergeCommitMap.get(path)))) ||
                    (commonPath == 7 &&
                            (!splitPointMap.get(path).equals(newCommitMap.get(path))) &&
                            (!splitPointMap.get(path).equals(mergeCommitMap.get(path))) &&
                            (!newCommitMap.get(path).equals(mergeCommitMap.get(path))))){
                conflict = true;
                String curBranchContents = "";
                if(newCommitMap.containsKey(path)){
                    Blob newCommitBlob = getBlobByID(newCommitMap.get(path));
                    curBranchContents = new String(newCommitBlob.getContents(), StandardCharsets.UTF_8);
                }

                String givenBranchContents = "";
                if (mergeCommitMap.containsKey(path)) {
                    Blob mergeCommitBlob = getBlobByID(mergeCommitMap.get(path));
                    givenBranchContents = new String(mergeCommitBlob.getContents(), StandardCharsets.UTF_8);
                }

                String conflictContents = "<<<<<<< HEAD\n" + curBranchContents + "=======\n" + givenBranchContents + ">>>>>>>\n";
                String fileName = getBlobByID(blobID).getFileName();
                File conflictFile = join(CWD, fileName);
                writeContents(conflictFile, conflictContents);
            }
        }

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }


    private static Commit caculateMergedCommit(Commit newCommit, List<String> overwriteFiles, List<String> writeFiles, List<String> deleteFiles){
        Map<String, String> mergedCommitBlobs = newCommit.getPathToBlobID();

        if(!overwriteFiles.isEmpty()){
            for(String blobID : overwriteFiles){
                Blob blob = getBlobByID(blobID);
                mergedCommitBlobs.put(blob.getFilePath(), blobID);
            }
        }

        if (!writeFiles.isEmpty()) {
            for (String blobID : writeFiles) {
                Blob b = getBlobByID(blobID);
                mergedCommitBlobs.put(b.getFilePath(), blobID);
            }
        }

        if (!deleteFiles.isEmpty()) {
            for (String blobID : overwriteFiles) {
                Blob b = getBlobByID(blobID);
                mergedCommitBlobs.remove(b.getFilePath());
            }
        }

        return new Commit(newCommit.getMessage(), mergedCommitBlobs, newCommit.getParents());
    }
}
