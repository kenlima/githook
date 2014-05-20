CommitChecker
======
git에서 변경된 파일에 대해서 정의된 주석에 맞게 재가공 함
커밋전에 한번 실행해 주거나 git hook 에 걸어주면 됨

예) 파일명이 aaa.java 일 경우이고 오늘 날짜가 2014. 5. 8 인 경우

[변경전 자바소스]
```java
@file : a.java
@date 생성 : 2014. 2. 4.
@date 최종수정 : 2014. 2. 1.
```
[변경후 자바소스]
```java
@file : aaa.java
@date 생성 : 2014-02-04
@date 최종수정 : 2014-02-01
```
기능
----
* @file :  설명에 원래 파일명으로 변경
* @date 생성 : 날짜 포매팅 변경
* @date 최종수정 : 오늘 날짜로 변경
* 푸쉬되면 안되는 파일들 체크해서 출력해 준다. ".classpath", ".settings", "pom.xml", ".project", "/bin", ".iml", ".springBeans", ".gitignore", "target", ".idea"


Third Party
-----------
* jgit
* joda-time
* commons-io

실행
----
java CommitChecker 워킹디렉토리위치

ex) java CommitChecker /Users/jwlee/workspace/admin_project

