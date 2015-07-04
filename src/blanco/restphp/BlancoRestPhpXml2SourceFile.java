/*
 * blanco Framework
 * Copyright (C) 2004-2006 IGA Tosiki
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.restphp;

import blanco.cg.BlancoCgObjectFactory;
import blanco.cg.BlancoCgSupportedLang;
import blanco.cg.transformer.BlancoCgTransformerFactory;
import blanco.cg.util.BlancoCgLineUtil;
import blanco.cg.valueobject.BlancoCgClass;
import blanco.cg.valueobject.BlancoCgField;
import blanco.cg.valueobject.BlancoCgMethod;
import blanco.cg.valueobject.BlancoCgSourceFile;
import blanco.commons.util.BlancoJavaSourceUtil;
import blanco.commons.util.BlancoNameAdjuster;
import blanco.commons.util.BlancoStringUtil;
import blanco.restphp.resourcebundle.BlancoRestPhpResourceBundle;
import blanco.restphp.valueobject.BlancoRestPhpFieldStructure;
import blanco.restphp.valueobject.BlancoRestPhpStructure;
import blanco.xml.bind.BlancoXmlBindingUtil;
import blanco.xml.bind.BlancoXmlUnmarshaller;
import blanco.xml.bind.valueobject.BlancoXmlDocument;
import blanco.xml.bind.valueobject.BlancoXmlElement;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 「メッセージ定義書」Excel様式からメッセージを処理するクラス・ソースコードを生成。
 * 
 * このクラスは、中間XMLファイルからソースコードを自動生成する機能を担います。
 * 
 * @author IGA Tosiki
 * @author tueda
 */
public class BlancoRestPhpXml2SourceFile {
    /**
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestPhpResourceBundle fBundle = new BlancoRestPhpResourceBundle();

    /**
     * 出力対象となるプログラミング言語。
     */
    private int fTargetLang = BlancoCgSupportedLang.PHP;

    /**
     * 内部的に利用するblancoCg用ファクトリ。
     */
    private BlancoCgObjectFactory fCgFactory = null;

    /**
     * 内部的に利用するblancoCg用ソースファイル情報。
     */
    private BlancoCgSourceFile fCgSourceFile = null;

    /**
     * 内部的に利用するblancoCg用クラス情報。
     */
    private BlancoCgClass fCgClass = null;

    /**
     * フィールド名やメソッド名の名前変形を行うかどうか。
     */
    private boolean fNameAdjust = true;

    /**
     * 自動生成するソースファイルの文字エンコーディング。
     */
    private String fEncoding = null;

    public void setEncoding(final String argEncoding) {
        fEncoding = argEncoding;
    }

    /**
     * 中間XMLファイルからソースコードを自動生成します。
     * 
     * @param argMetaXmlSourceFile
     *            メタ情報が含まれているXMLファイル。
     * @param argDirectoryTarget
     *            ソースコード生成先ディレクトリ (/mainを除く部分を指定します)。
     * @param argNameAdjust
     *            名前変形を行うかどうか。
     * @throws IOException
     *             入出力例外が発生した場合。
     */
    public void process(final File argMetaXmlSourceFile,
            final boolean argNameAdjust, final File argDirectoryTarget)
            throws IOException {

        System.out.println("BlancoRestPhpXml2SourceFile#process file = " + argMetaXmlSourceFile.getName());

        fNameAdjust = argNameAdjust;

        // メタ情報を解析してバリューオブジェクトのツリーを取得します。
        final BlancoXmlDocument documentMeta = new BlancoXmlUnmarshaller()
                .unmarshal(argMetaXmlSourceFile);

        // ルートエレメントを取得します。
        final BlancoXmlElement elementRoot = BlancoXmlBindingUtil
                .getDocumentElement(documentMeta);
        if (elementRoot == null) {
            // ルートエレメントが無い場合には処理中断します。
            return;
        }

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(elementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // まずは電文を処理します．
            // 共通情報を取得します。
            final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle
                            .getMeta2xmlTelegramCommon());
            if (elementCommon == null) {
                // commonが無い場合には、このシートの処理をスキップします。
                continue;
            }

            final String name = BlancoXmlBindingUtil.getTextContent(
                    elementCommon, "name");

            if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
                // nameが空の場合には処理をスキップします。
                continue;
            }

            // 一覧情報を取得します。
            final BlancoXmlElement elementList = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle.getMeta2xmlTeregramList());

            // シートから詳細な情報を取得します。
            final BlancoRestPhpStructure processStructure = parseSheet(
                    elementCommon, elementList, argDirectoryTarget);

            if (processStructure != null) {
                // メタ情報の解析結果をもとにソースコード自動生成を実行します。
                process(processStructure, argDirectoryTarget);
            }
        }
    }

    /**
     * sheetエレメントを展開します。
     * 
     * @param argElementCommon
     *            現在処理しているCommonノード。
     * @param argElementList
     *            現在処理しているListノード。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     * @return 収集されたメタ情報構造データ。
     */
    private BlancoRestPhpStructure parseSheet(
            final BlancoXmlElement argElementCommon,
            final BlancoXmlElement argElementList, final File argDirectoryTarget) {

        final BlancoRestPhpStructure processStructure = new BlancoRestPhpStructure();
        processStructure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        processStructure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));

        if (BlancoStringUtil.null2Blank(processStructure.getPackage()).trim()
                .length() == 0) {
            throw new IllegalArgumentException(fBundle
                    .getXml2sourceFileErr001(processStructure.getName()));
        }

        if (BlancoXmlBindingUtil
                .getTextContent(argElementCommon, "description") != null) {
            processStructure.setDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "description"));
        }

        if (BlancoXmlBindingUtil.getTextContent(argElementCommon,
                "fileDescription") != null) {
            processStructure.setFileDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "fileDescription"));
        }

        if (argElementList == null) {
            return null;
        }

        // 一覧の内容を取得します。
        final List<BlancoXmlElement> listField = BlancoXmlBindingUtil
                .getElementsByTagName(argElementList, "field");
        for (int indexField = 0; indexField < listField.size(); indexField++) {
            final Object nodeField = listField.get(indexField);

            if (nodeField instanceof BlancoXmlElement == false) {
                continue;
            }

            final BlancoXmlElement elementField = (BlancoXmlElement) nodeField;
            BlancoRestPhpFieldStructure field = new BlancoRestPhpFieldStructure();
            field
                    .setNo(BlancoXmlBindingUtil.getTextContent(elementField,
                            "no"));

            field.setName(BlancoXmlBindingUtil.getTextContent(elementField,
                    "name"));
            if (BlancoStringUtil.null2Blank(field.getName()).length() == 0) {
                continue;
            }

            // 既に同じ内容が登録されていないかどうかのチェック。
            for (int indexPast = 0; indexPast < processStructure.getListField()
                    .size(); indexPast++) {
                final BlancoRestPhpFieldStructure fieldPast = processStructure
                        .getListField().get(indexPast);
                if (fieldPast.getName().equals(field.getName())) {
                    throw new IllegalArgumentException(
                            fBundle.getXml2sourceFileErr003(processStructure
                                    .getName(), field.getName()));
                }
            }

            field.setType(BlancoXmlBindingUtil.getTextContent(elementField,
                    "type"));
            if (BlancoStringUtil.null2Blank(field.getType()).length() == 0) {
                // ここで異常終了。
                continue;
            }

            field.setDefault(BlancoXmlBindingUtil.getTextContent(elementField,
                    "default"));

            field.setDescription(BlancoXmlBindingUtil.getTextContent(
                    elementField, "description"));

            processStructure.getListField().add(field);
        }

        return processStructure;
    }

    /**
     * 収集された情報を元に、ソースコードを自動生成します。
     * 
     * @param argProcessStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void process(
            final BlancoRestPhpStructure argProcessStructure,
            final File argDirectoryTarget) {

        // 従来と互換性を持たせるため、/mainサブフォルダに出力します。
        final File fileBlancoMain = new File(argDirectoryTarget
                .getAbsolutePath()
                + "/main");

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(argProcessStructure
                .getPackage(), "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(argProcessStructure.getName(),
                BlancoStringUtil.null2Blank(argProcessStructure
                        .getDescription()));
        fCgSourceFile.getClassList().add(fCgClass);

        if (argProcessStructure.getFileDescription() != null) {
            fCgSourceFile.setDescription(argProcessStructure
                    .getFileDescription());
        }

        expandValueObject(argProcessStructure);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }

    /**
     * バリューオブジェクトを展開します。
     * 
     * @param argProcessStructure
     *            メタファイルから収集できた処理構造データ。
     */
    private void expandValueObject(
            final BlancoRestPhpStructure argProcessStructure) {

        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestPhpFieldStructure fieldLook = argProcessStructure
                    .getListField().get(indexField);

            expandField(argProcessStructure, fieldLook);

            expandMethodSet(argProcessStructure, fieldLook);

            expandMethodGet(argProcessStructure, fieldLook);
        }

        expandMethodToString(argProcessStructure);
    }

    /**
     * フィールドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandField(
            final BlancoRestPhpStructure argProcessStructure,
            final BlancoRestPhpFieldStructure fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgField cgField = fCgFactory.createField("f" + fieldName,
                fieldLook.getType(), "");
        fCgClass.getFieldList().add(cgField);
        cgField.setAccess("private");

        cgField.setDescription(fBundle.getXml2sourceFileFieldName(fieldLook
                .getName()));
        cgField.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileFieldType(fieldLook.getType()));
        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgField.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        if (fieldLook.getDefault() != null) {
            if (fieldLook.getType().equals("string")) {
                // クオートを付与します。
                cgField.setDefault(BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang)
                        + BlancoJavaSourceUtil
                                .escapeStringAsJavaSource(fieldLook
                                        .getDefault())
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang));
            } else if (fieldLook.getType().equals("boolean")
                    || fieldLook.getType().equals("integer")
                    || fieldLook.getType().equals("float")
                    || fieldLook.getType().equals("double")) {
                cgField.setDefault(fieldLook.getDefault());
            } else {
                throw new IllegalArgumentException(fBundle
                        .getXml2sourceFileErr006(argProcessStructure.getName(),
                                fieldLook.getName(), fieldLook.getDefault(),
                                fieldLook.getType()));
            }
        }
    }

    /**
     * setメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodSet(
            final BlancoRestPhpStructure argProcessStructure,
            final BlancoRestPhpFieldStructure fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("set"
                + fieldName, fBundle.getXml2sourceFileSetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileSetLangdoc02(fieldLook.getType()));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        cgMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + fieldName, fieldLook
                        .getType(), fBundle
                        .getXml2sourceFileSetArgLangdoc(fieldLook.getName())));

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                + "this->f" + fieldName + " = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg"
                + fieldName + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * getメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodGet(
            final BlancoRestPhpStructure argProcessStructure,
            final BlancoRestPhpFieldStructure fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("get"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileGetLangdoc02(fieldLook.getType()));

        cgMethod.setReturn(fCgFactory.createReturn(fieldLook.getType(), fBundle
                .getXml2sourceFileGetReturnLangdoc(fieldLook.getName())));

        if (fieldLook.getDefault() != null) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fBundle.getXml2sourceFileGetArgLangdoc(fieldLook
                            .getDefault()));
        }

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "this->" + "f" + fieldName
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * toStringメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodToString(
            final BlancoRestPhpStructure argProcessStructure) {
        final BlancoCgMethod method = fCgFactory.createMethod("__toString",
                "このバリューオブジェクトの文字列表現を取得します。");
        fCgClass.getMethodList().add(method);

        method.getLangDoc().getDescriptionList().add(
                "オブジェクトのシャロー範囲でしかtoStringされない点に注意して利用してください。");
        method
                .setReturn(fCgFactory.createReturn("string",
                        "バリューオブジェクトの文字列表現。"));

        final List<String> listLine = method.getLineList();

        listLine.add(BlancoCgLineUtil.getVariableDeclaration(fTargetLang,
                "buf", "string", BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang)
                        + BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang))
                + BlancoCgLineUtil.getTerminator(fTargetLang));

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + argProcessStructure.getPackage() + "."
                + argProcessStructure.getName() + "["
                + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestPhpFieldStructure fieldLook = argProcessStructure
                    .getListField().get(indexField);

            String fieldName = fieldLook.getName();
            if (fNameAdjust) {
                fieldName = BlancoNameAdjuster.toClassName(fieldName);
            }

            if (fieldLook.getType().equals("array") == false) {
                String strLine = BlancoCgLineUtil
                        .getVariablePrefix(fTargetLang)
                        + "buf = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "buf "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + (indexField == 0 ? "" : ",")
                        + fieldLook.getName()
                        + "="
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " ";
                if (fieldLook.getType().equals("string")) {
                    strLine += BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this->f" + fieldName;
                } else if (fieldLook.getType().equals("boolean")) {
                    strLine += "("
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this->f" + fieldName + " ? 'true' : 'false')";
                } else {
                    strLine += "(string) "
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this->f" + fieldName;
                }
                strLine += BlancoCgLineUtil.getTerminator(fTargetLang);
                listLine.add(strLine);
            } else {
                listLine.add("// TODO 配列は未対応です。");
            }
        }

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + "]" + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        listLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf"
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }
}
