package org.vmy;

public class MainCore {

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            throw new Exception("You must provide mainClass to run as a parameter.");
        }
        final String mainName = args[0];
        switch (mainName) {
            case "CheckUpdater":
                CheckUpdater.main(args);
                break;
            case "FileWatcher":
                FileWatcher.main(args);
                break;
            case "ParseBot":
                ParseBot.main(args);
                break;
            case "GraphBot":
                GraphBot.main(args);
                break;
            default:
                throw new Exception("Unknown mainClass: " + mainName);
        }
    }
}
