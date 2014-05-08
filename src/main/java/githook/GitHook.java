package githook;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.nyu.cs.javagit.api.JavaGitConfiguration;
import edu.nyu.cs.javagit.api.JavaGitException;
import edu.nyu.cs.javagit.api.commands.GitStatus;
import edu.nyu.cs.javagit.api.commands.GitStatusResponse;

public class GitHook {
    private File toCommitFile;
    private List<String> lines;

    public GitHook(File toCommitFile) throws IOException {
        this.toCommitFile = toCommitFile;
        this.lines = FileUtils.readLines(toCommitFile, "UTF-8");
    }

    public static String parseDate(String date) {

        String[] formaters = new String[] { "yyyyMMdd", "yyyy-MM-dd", "yyyy. MM. dd.", "yyyy.MM.dd", "yyyy. MM. dd" };

        DateTime dt = null;
        for (String format : formaters) {
            try {
                DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
                dt = fmt.parseDateTime(date);

                break;

            } catch (Exception e) {
                //System.out.println("[WARN] format not matching... " + format);
            }
        }

        if (dt == null) {
            throw new IllegalArgumentException("All format not matching...");
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        String result = fmt.print(dt);

        return result;
    }

    public static void main(String[] args) throws IOException, JavaGitException {

        JavaGitConfiguration.setGitPath("/usr/local/git/bin/");
        System.out.println("git version : " + JavaGitConfiguration.getGitVersion());

        File workingTreePath = new File("/Users/jwlee/wemakeprice/workspace/admin_project");
        System.out.println("git path : " + workingTreePath.getPath());

        GitStatus gitStatus = new GitStatus();
        GitStatusResponse status = gitStatus.status(workingTreePath);

        int untractedFileSize = status.getUntrackedFilesSize();
        int modifiedFileSize = status.getModifiedFilesToCommitSize();
        int newFileSize = status.getNewFilesToCommitSize();
        System.out.println("untractedFileSize : " + untractedFileSize);
        System.out.println("modifiedFileSize : " + modifiedFileSize);
        System.out.println("newFileSize : " + newFileSize);

        List<File> modifiedOrNewFiles = scanModifiedOrNewFiles(status);

        for (File file : modifiedOrNewFiles) {
            GitHook gitHook = new GitHook(file);
            gitHook.updateMetaData();
        }

    }

    private static List<File> scanModifiedOrNewFiles(GitStatusResponse status) {
        Iterable<File> untractedFiles = status.getUntrackedFiles();
        Iterable<File> modifiedFiles = status.getModifiedFilesToCommit();
        Iterable<File> newFiles = status.getNewFilesToCommit();

        Map<String, File> files = new HashMap<String, File>();
        for (File file : untractedFiles) {
            files.put(file.getPath(), file);
        }
        for (File file : modifiedFiles) {
            files.put(file.getPath(), file);
        }

        for (File file : newFiles) {
            files.put(file.getPath(), file);
        }
        return new ArrayList<File>(files.values());
    }

    private void updateMetaData() throws IOException {
        int idx = 0;
        for (String line : lines) {
            int commentIdx = searchCommentIdx(line);

            if (commentIdx > 0) {
                updateMessage("@file", idx, line);
                updateMessage("@date 최종수정", idx, line);
                updateMessage("@date 생성", idx, line);
            }
            idx++;
        }
        FileUtils.writeLines(toCommitFile, lines);
    }

    private void updateMessage(String keyword, int idx, String line) {
        int keywordIdx = line.indexOf(keyword);
        if (keywordIdx < 0) {
            return;
        }
        /*
        // 정상적인 파일명이면
        if (line.indexOf(toCommitFile.getName(), fileIdx) > -1) {
            return;
        }
        */

        // 쓰기
        String start = line.substring(0, keywordIdx + keyword.length());

        String append = "";
        boolean success = true;
        if (keyword.equals("@file")) {
            append = " " + toCommitFile.getName();
        } else if (keyword.equals("@date 최종수정")) {
            append = " : " + getCurrentDate();
        } else if (keyword.equals("@date 생성")) {
            int colIdx = line.indexOf(":", keywordIdx);
            if (colIdx > 0) {
                String currDate = line.substring(colIdx + 1);
                String toDate = parseDate(currDate.trim());
                append = " : " + toDate;
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        if (success) {
            lines.set(idx, start + append);
            System.out.println(keyword + " updated. " + toCommitFile.getPath());
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Date currentTime = new Date();
        String mTime = mSimpleDateFormat.format(currentTime);
        return mTime;
    }

    private static int searchCommentIdx(String string) {
        int a = string.indexOf("/**");
        int b = string.indexOf("*");

        int c = a;
        if (a < b) {
            c = b;
        }
        return c;
    }
}
