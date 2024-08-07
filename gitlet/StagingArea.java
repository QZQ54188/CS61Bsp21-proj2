package gitlet;

import java.io.File;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gitlet.Repository.OBJECT_DIR;
import static gitlet.Utils.*;

public class StagingArea implements Serializable{
    private Map<String, String> pathToBlobID = new HashMap<>();

    public boolean isNewBlob(Blob blob){
        return !pathToBlobID.containsValue(blob.getBlobID());
    }

    public Map<String, String> getPathToBlobID(){
        return pathToBlobID;
    }

    public boolean isFilePathExists(String path){
        return pathToBlobID.containsKey(path);
    }

    public void delete(Blob blob){
        pathToBlobID.remove(blob.getFilePath());
    }

    public void delete(String path) {
        pathToBlobID.remove(path);
    }

    public void add(Blob blob){
        pathToBlobID.put(blob.getFilePath(), blob.getBlobID());
    }

    public void saveAddStage(){
        writeObject(Repository.ADDSTAGE_FILE, this);
    }

    public void saveRemoveStage(){
        writeObject(Repository.REMOVESTAGE_FILE, this);
    }

    public void clear(){
        pathToBlobID.clear();
    }

    public List<Blob> getBlobList(){
        List<Blob> blobList = new ArrayList<>();
        for(String id : pathToBlobID.values()){
            Blob blob = getBlobByID(id);
            blobList.add(blob);
        }
        return blobList;
    }

    public static Blob getBlobByID(String id){
        File blobFile = join(OBJECT_DIR, id);
        return readObject(blobFile, Blob.class);
    }

    public boolean exists(String filePath){
        return pathToBlobID.containsKey(filePath);
    }

    public boolean isEmpty(){
        return getPathToBlobID().isEmpty();
    }

}
