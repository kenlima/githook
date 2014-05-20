package githook;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class CommitChecker {
    private File workingDir;
    private List<String> lines;
    private Git git;
    private File toCommitFile;

    private static final String[] exceptFiles = { ".classpath", ".settings", "pom.xml", ".project", "/bin", ".iml", ".springBeans", ".gitignore", "target",
            ".idea" };

    public CommitChecker(String workingDir) throws IOException {
        this.workingDir = new File(workingDir);
        this.git = Git.open(this.workingDir);
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.out.println("Usage : java CommitChecker <git working tree path>");
            System.out.println("ex : java CommitChecker /Users/jwlee/wemakeprice/workspace/admin_project");
            System.exit(0);
        }

        try {
            CommitChecker commitChecker = new CommitChecker(args[0]);
            commitChecker.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute() throws NoWorkTreeException, GitAPIException, IOException {

        Set<String> uc = git.status().call().getUncommittedChanges();
        Set<String> ut = git.status().call().getUntracked();

        uc.addAll(ut);

        System.out.println("To commit file count : " + uc.size());

        List<String> exceptFiles = new ArrayList<>();

        for (String pathString : uc) {
            File toCommitFile = new File(workingDir, pathString);

            if (isExceptFile(toCommitFile)) {
                exceptFiles.add(pathString);
            } else {
                scan(toCommitFile);
            }
        }

        if (!exceptFiles.isEmpty()) {
            System.out.println("Except file detected.");
            for (String file : exceptFiles) {
                System.out.println("Except file : " + file);
            }
        }

    }

    /**
     * @brief
     * @details
     * @param
     * @return
     */
    private boolean isExceptFile(File toCommitFile) {
        for (String file : exceptFiles) {
            if (toCommitFile.getPath().indexOf(file) > -1) {
                return true;
            }
        }
        return false;

    }

    private void scan(File toCommitFile) throws IOException {
        int idx = 0;
        this.lines = FileUtils.readLines(toCommitFile);
        this.toCommitFile = toCommitFile;

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
            System.out.println(keyword + " replaced. " + toCommitFile.getPath());
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Date currentTime = new Date();
        String mTime = mSimpleDateFormat.format(currentTime);
        return mTime;
    }

    private int searchCommentIdx(String string) {
        int a = string.indexOf("/**");
        int b = string.indexOf("*");

        int c = a;
        if (a < b) {
            c = b;
        }
        return c;
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
}
