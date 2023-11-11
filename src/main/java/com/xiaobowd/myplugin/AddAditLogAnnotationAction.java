package com.xiaobowd.myplugin;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import java.util.Locale;

public class AddAditLogAnnotationAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        IdeView view = event.getRequiredData(LangDataKeys.IDE_VIEW);
        PsiDirectory directory = view.getOrChooseDirectory();
        if (directory == null) {
            return;
        }
        for (PsiFile file : directory.getFiles()) {
            if (!(file instanceof PsiJavaFile)) {
                continue;
            }
            System.out.println(file.getName());
            VirtualFile virtualFile = file.getVirtualFile();

            PsiJavaFile psiJavaFile = (PsiJavaFile) file;

            PsiClass psiJavaClass = psiJavaFile.getClasses()[0];
            PsiAnnotation[] annotations = psiJavaClass.getAnnotations();
            if (annotations == null || annotations.length == 0) {
                continue;
            }
            boolean isJalorResource = false;
            String resourceDesc = "";
            for (PsiAnnotation annotation : annotations) {
                if (annotation.getQualifiedName().equals("org.example.JalorResource")) {
                    resourceDesc = annotation.findAttributeValue("desc").getText();
                    isJalorResource = true;
                    System.out.println("类上有JalorResource注解");
                    if (annotation.hasAttribute("code")) {
                        System.out.println(annotation.findAttributeValue("code").getText() + " : " + annotation.findAttributeValue("desc").getText());
                    }
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        System.out.println("增加注解引用");

                        PsiElementFactory elementFactory = PsiElementFactory.getInstance(event.getProject());
                        PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand("org.xiaobowd.com");
                        file.add(importStatement);
                    });
                }
            }

            if (!isJalorResource) {
                continue;
            }

            for (PsiMethod method : psiJavaClass.getMethods()) {
                if (method.hasAnnotation("org.example.JalorOperation")) {
                    PsiAnnotation annotation = method.getAnnotation("org.example.JalorOperation");
                    System.out.println("方法上有JalorOperation注解");
                    System.out.println(annotation.findAttributeValue("code").getText() + " : " + annotation.findAttributeValue("desc").getText());

                    String finalResourceDesc = resourceDesc;
                    WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {
                        System.out.println("开始增加注解");
                        PsiParameterList parameterList = method.getParameterList();
                        StringBuffer stringBuffer = new StringBuffer();
//                        @JalorOperation(code  = "JalorOperationCode", desc = "JalorOperationDesc")
                        stringBuffer.append("@Audit(code  = \"");

                        for (PsiParameter parameter : parameterList.getParameters()) {
                            stringBuffer.append(String.format(Locale.ROOT, "#{%s}", parameter.getName()));
                        }

                        stringBuffer.append("\", desc = ");
                        stringBuffer.append(finalResourceDesc);
                        stringBuffer.append(")");

                        PsiElementFactory elementFactory = PsiElementFactory.getInstance(event.getProject());
                        PsiAnnotation annotationFromText = elementFactory.createAnnotationFromText(stringBuffer.toString(), method);
                        method.addAfter(annotationFromText, annotation);
                    });
                }
            }
        }
    }
}
