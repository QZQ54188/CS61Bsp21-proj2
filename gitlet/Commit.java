package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Repository.CWD;
import static gitlet.Utils.*;
import static gitlet.Repository.OBJECT_DIR;
import static gitlet.StagingArea.getBlobByID;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private String message;
    private Date time;
    private String commitHash;
    private List<String> parents;
    private File commitSaveFile;
    private Map<String, String> pathToBlobID = new HashMap<>();

    private String timeStamp;

    public Commit(String message, Map<String, String> pathToBlobID, List<String> parents){
        this.message = message;
        this.parents = parents;
        this.pathToBlobID = pathToBlobID;
        this.time = new Date();
        this.commitHash = generateHash();
        this.commitSaveFile = generateFile();
    }

    public Commit(){
        this.message = "initial commit";
        this.parents = new ArrayList<>();
        this.pathToBlobID = new HashMap<>();
        this.time = new Date(0);
        this.commitHash = generateHash();
        this.commitSaveFile = generateFile();
        this.timeStamp = timeToTimeStamp(time);
    }

    private String generateTimeStamp(){
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.CHINA);
        return dateFormat.format(time);
    }

    private String generateHash(){
        return sha1(generateTimeStamp(), message, parents.toString(), pathToBlobID.toString());
    }

    private File generateFile(){
        return join(OBJECT_DIR, commitHash);
    }

    public String getMessage(){
        return message;
    }

    public Map<String, String> getPathToBlobID(){
        return pathToBlobID;
    }

    public List<String> getBlobIDList(){
        List<String> res = new ArrayList<>(pathToBlobID.values());
        return res;
    }

    public String getCommitHash(){
        return commitHash;
    }

    public Date getTime(){
        return time;
    }

    public void save(){
        writeContents(commitSaveFile, this);
    }

    public boolean exists(String filePath){
        return pathToBlobID.containsKey(filePath);
    }

    public List<String> getParents(){
        return parents;
    }

    private static String timeToTimeStamp(Date time){
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.CHINA);
        return dateFormat.format(time);
    }

    public String getTimeStamp(){
        return timeStamp;
    }

    public List<String> getFileNames(){
        List<String> fileNames = new ArrayList<>();
        List<Blob> blobs = getBlobList();
        for(Blob blob : blobs){
            fileNames.add(blob.getFileName());
        }
        return fileNames;
    }

    private List<Blob> getBlobList(){
        List<Blob> blobList = new ArrayList<>();
        for(String id : pathToBlobID.values()){
            Blob blob = getBlobByID(id);
            blobList.add(blob);
        }
        return blobList;
    }

    public Blob getBlobByFileName(String fileName){
        File file = join(CWD, fileName);
        String path = file.getPath();
        String blobID = pathToBlobID.get(path);
        return getBlobByID(blobID);
    }
}
