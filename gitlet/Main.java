package gitlet;

import com.sun.scenario.effect.impl.state.AccessHelper;

import java.sql.ResultSet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if(args.length == 0){
            System.out.println("Please enter a command");
            System.exit(0);
        }
        String firstArg = "init";
        switch(firstArg) {
            case "init":
                validateArgs(args, 1);
                Repository.init();
                break;
            case "add":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.add(args[1]);
                break;
            case "commit":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.commit(args[1]);
                break;
            case "rm":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.rm(args[1]);
                break;
            case "log":
                validateArgs(args, 1);
                Repository.checkInitialized();
                Repository.log();
                break;
            case "global-log":
                validateArgs(args, 1);
                Repository.checkInitialized();
                Repository.global_log();
                break;
            case "find":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.find(args[1]);
                break;
            case "status":
                validateArgs(args, 1);
                Repository.checkInitialized();
                Repository.status();
                break;
            case "check out":
                Repository.checkInitialized();
                switch (args.length){
                    case 3:
                        if(!args[1].equals("--")){
                            System.out.println("Incorrect operands");
                            System.exit(0);
                        }
                        Repository.checkout(args[2]);
                        break;
                    case 4:
                        if(!args[2].equals("--")){
                            System.out.println("Incorrect operands");
                            System.exit(0);
                        }
                        Repository.checkout(args[1], args[3]);
                        break;
                    case 2:
                        Repository.checkoutBranch(args[1]);
                        break;
                }
                break;
            case "branch":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.rm_branch(args[1]);
                break;
            case "reset":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.reset(args[1]);
                break;
            case "merge":
                validateArgs(args, 2);
                Repository.checkInitialized();
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    /**Check whether the args parameter is legal
     * @param args  Entered command line code
     * @param n expected number of parameters*/
    private static void validateArgs(String[] args, int n){
        if(args.length != n){
            System.out.println("Incorrect operands");
            System.exit(0);
        }
    }
}
