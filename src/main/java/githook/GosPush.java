package githook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;


public class GosPush {
    private static FileUtils fileUtils = new FileUtils();
    private static String workingDir;
    private static String copyTempDir;
    
	public static void main(String[] args) throws Exception {
	    // 설정 정의.
        workingDir = new String("/Users/we/wemakeprice/workspace/admin_project");
        copyTempDir = new String("/Users/we/temp");
        String origBranchName = new String("develop");
        String targetBranchName = new String("develop_gos");
        String commitMessage = new String("테스트 커밋");
        
        File copyTempDirFile = new File(copyTempDir);
        File workingDirFile = new File(workingDir);
        List<String> copyFileList = new ArrayList<>();    // 복사할 파일 리스트. 디렉토리, 파일 모두.
        List<String> exceptFileList = new ArrayList<>();  // add 하지 않을 파일 리스트.

        // 복사할 파일 및 디렉토리 설정.
        copyFileList.add(new String("/src"));
        copyFileList.add(new String("/pom.xml"));
        
        // add 하지 않을 파일 및 디렉토리 설정.
        exceptFileList.add(new String("/.classpath"));
        exceptFileList.add(new String("/.gitignore"));
        exceptFileList.add(new String("/.project"));
        exceptFileList.add(new String("/bin"));
        exceptFileList.add(new String("/src/test/java/com/wemakeprice/administrator"));
        
        // jgit 초기화.
	    Git git = Git.open(workingDirFile);
	    
	    // 복사할 원본 브랜치 이동.
        branchCheckout(git, origBranchName);
        
        // 임시 디렉토리 생성.
        emptyMkdir(copyTempDirFile);
        
        // 복사할 파일 및 리스트 복사.
        copyFiles(copyFileList, workingDir, copyTempDir);
        
        // 브랜치를 타켓브랜치로 변경.
        branchCheckout(git, targetBranchName);
        
        // 임시 파일들을 복사
        copyFiles(copyFileList, copyTempDir, workingDir);
        
        // untracked file list 가져오기. exceptFileList 제외.
	    Set<String> unTrackedFiles = getUntrachedFiles(git, exceptFileList);
	    
	    // untracked file list git add
	    addUntrackedFiles(git, unTrackedFiles);
	    
	    // commit.
	    gitCommit(git, commitMessage);
        
	}

    /**
     * @brief
     * @details
     * @param
     * @return
     * @throws
     */
    private static void gitCommit(Git git, String commitMessage) {
        CommitCommand commit = git.commit();
        try {
            commit.setMessage(commitMessage).call();
        } catch (NoHeadException e) {
            e.printStackTrace();
        } catch (NoMessageException e) {
            e.printStackTrace();
        } catch (UnmergedPathsException e) {
            e.printStackTrace();
        } catch (ConcurrentRefUpdateException e) {
            e.printStackTrace();
        } catch (WrongRepositoryStateException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief
     * @details
     * @param
     * @return
     * @throws
     */
    private static void addUntrackedFiles(Git git, Set<String> unTrackedFiles) {
        for (String file : unTrackedFiles) {
            try {
                AddCommand add = git.add();
                add.addFilepattern(file).call();
                System.out.println(file);
            } catch (NoFilepatternException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param exceptFileList 
     * @brief
     * @details
     * @param
     * @return
     * @throws
     */
    private static Set<String> getUntrachedFiles(Git git, List<String> exceptFileList) {
        Set<String> result = new HashSet<>();
        try {
            Set<String> all = new HashSet<>();
            
            List<Set<String>> list = new ArrayList<>();
            list.add(git.status().call().getUntracked());
            list.add(git.status().call().getModified());
            list.add(git.status().call().getAdded());
            list.add(git.status().call().getRemoved());
            list.add(git.status().call().getChanged());
            list.add(git.status().call().getMissing());
            
            addSetListSet(all, list);
            
            System.out.println(all.size());
            
            for (String fileName : all) {
                System.out.println("untracked file name = " + fileName);
                boolean isExceptFile = false;
                for (String exFileName : exceptFileList) {
                    if("/".concat(fileName).indexOf(exFileName) == 0){
                        isExceptFile = true;
                        break;
                    }
                }
                if(!isExceptFile){
                    System.out.println("add filename = " + fileName);
                    result.add(fileName);
                }
            }
        } catch (NoWorkTreeException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @brief
     * @details
     * @param
     * @return
     * @throws
     */
    private static void addSetListSet(Set<String> all, List<Set<String>> list) {
        for (Set<String> set : list) {
            for (String str : set) {
                all.add(str);
            }
        }
    }

    /**
     * @brief
     * @details
     * @param
     * @return
     * @throws
     */
    private static void branchCheckout(Git git, String branchName) {
        try {
            git.checkout().setName(branchName).call();
        } catch (RefAlreadyExistsException e) {
            e.printStackTrace();
        } catch (RefNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidRefNameException e) {
            e.printStackTrace();
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @brief 복사할 파일 리스트와 원본, 대상 경로를 입력받아 복사하는 함수
     * @details
     * @param copyFileList -> origPathStr을 상대경로로 하는 파일들의 경로.
     * origPathStr -> 복사할 파일들의 최상위 경로로 상대경로로 사용된다.
     * targetPathStr -> 복사의 대상이 되는 경로의 최상위 경로.
     * @return
     * @throws
     */
    private static void copyFiles(List<String> copyFileList, String origPathStr, String targetPathStr) {
        for (String origFileName : copyFileList) {
            File origFile = new File(origPathStr.concat(File.separator).concat(origFileName));
            File tempFile = new File(targetPathStr.concat(File.separator).concat(origFileName));
            copyFile(origFile, tempFile);
        }
    }

    /**
     * @brief 파일이건 디렉토리건 입력받아 복사를 한다.
     * @details
     * @param origFile -> 복사의 원본 파일.
     * copyFile -> 복사할 경로의 파일 오브젝트.
     * @return
     * @throws
     */
    @SuppressWarnings("static-access")
    private static void copyFile(File origFile, File copyFile) {
        if(origFile.exists()){
            if (origFile.isDirectory()) {
                try {
                    fileUtils.copyDirectory(origFile, copyFile);
                } catch (IOException e) {
                    System.out.println("디렉토리 복사 중에 IOException 발생.");
                    e.printStackTrace();
                }
            }else{
                try {
                    fileUtils.copyFile(origFile, copyFile);
                } catch (IOException e) {
                    System.out.println("파일 복사 중에 IOException 발생.");
                    e.printStackTrace();
                }
            }
        }
        
    }

    
    
    /* 디렉토리를 생성해주고 그 내부를 비워주는 함수.
     * */
    private static void emptyMkdir(File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        try {
            FileUtils.cleanDirectory(targetDir);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
	
	
	

}