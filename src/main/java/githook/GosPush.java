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
        
        // unstaged file list 가져오기.
	    Set<String> unStagedFiles = getUnstagedFiles(git);
	    
	    // conflicted file list 가져오기.
        Set<String> conflictedFiles = getConflictedFiles(git);
        
	    // unstaged file list git add
	    addGitListExcetpList(git, unStagedFiles, exceptFileList);
	    
	    if(conflictedFiles.size() > 0){
	        System.out.println("충돌난 파일이 있습니다. 해결하고 커밋하세요.");
	    }else{
	        // commit.
	        gitCommit(git, commitMessage);
	        System.out.println("커밋이 완료되었습니다. 확인하세요.");
	    }
	}

    /**
     * @param exceptFileList 
     * @brief  exceptFileList 를 제외한 컨플릭트 리스트 반환.
     * @details 
     * @param
     * @return
     * @throws
     */
    private static Set<String> getConflictedFiles(Git git) {
        Set<String> result = new HashSet<>();
        try {
            result = git.status().call().getConflicting();
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
     * @param exceptFileList 
     * @brief files 리스트를 예외 파일 리스트와 대조하여 예외 리스트에 없는 파일만 git add 를 실행한다.
     * @details
     * @param
     * @return
     * @throws
     */
    private static void addGitListExcetpList(Git git, Set<String> files, List<String> exceptFileList) {
        for (String fileName : files) {
            boolean isExceptFile = false;
            for (String exFileName : exceptFileList) {
                if ("/".concat(fileName).indexOf(exFileName) == 0) {
                    isExceptFile = true;
                    break;
                }
            }
            if (!isExceptFile) {
                addGitFile(git, fileName);
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
    private static void addGitFile(Git git, String file) {
        try {
            AddCommand add = git.add();
            add.addFilepattern(file).call();
            System.out.println("git Add File ==> " + file);
        } catch (NoFilepatternException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
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
    private static Set<String> getUnstagedFiles(Git git) {
        Set<String> result = new HashSet<>();
        try {
            List<Set<String>> list = new ArrayList<>();
            
            list.add(git.status().call().getUntracked());
            list.add(git.status().call().getModified());
            list.add(git.status().call().getAdded());
            list.add(git.status().call().getRemoved());
            list.add(git.status().call().getChanged());
            list.add(git.status().call().getMissing());
            
            addSetListSet(result, list);
            
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
            addSet(all, set);
        }
    }
    
    /* Set 형식의 모든 모든 것을 받아서 합쳐주는 함수. */
    private static <T extends Object> void addSet(Set<T> orig, Set<T> target) {
        for (T str : target) {
            orig.add(str);
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