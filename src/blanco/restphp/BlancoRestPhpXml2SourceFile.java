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
import blanco.cg.valueobject.*;
import blanco.commons.util.BlancoJavaSourceUtil;
import blanco.commons.util.BlancoNameAdjuster;
import blanco.commons.util.BlancoStringUtil;
import blanco.restphp.resourcebundle.BlancoRestPhpResourceBundle;
import blanco.restphp.valueobject.BlancoRestPhpTelegram;
import blanco.restphp.valueobject.BlancoRestPhpTelegramField;
import blanco.restphp.valueobject.BlancoRestPhpTelegramProcess;
import blanco.valueobject.resourcebundle.BlancoValueObjectResourceBundle;
import blanco.xml.bind.BlancoXmlBindingUtil;
import blanco.xml.bind.BlancoXmlUnmarshaller;
import blanco.xml.bind.valueobject.BlancoXmlDocument;
import blanco.xml.bind.valueobject.BlancoXmlElement;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
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
     * 要求電文のベースクラス
     */
    private String inputTelegramBase = null;
    /**
     * 応答電文のベースクラス
     */
    private String outputTelegramBase = null;

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
            System.out.println("BlancoRestPhpXmlSourceFile#process !!! NO ROOT ELEMENT !!!");
            return;
        }

        // まずは電文を生成します．
        ArrayList<BlancoRestPhpTelegram> listTelegram = new ArrayList<>();
        processTelegram(argDirectoryTarget, elementRoot, listTelegram);

        // 次に電文処理を生成します
        processTelegramProcess(argDirectoryTarget, elementRoot, listTelegram);
    }

    private void processTelegramProcess(final File argDirectoryTarget, BlancoXmlElement elementRoot, List<BlancoRestPhpTelegram> argListTelegrams) {

        //Commonプロパティブロックが見つかったかどうか
        Boolean blnFoundElementCommon = false;

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(elementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // 共通情報を取得します。
            final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle
                            .getMeta2xmlProcessCommon());
            if (elementCommon == null) {
                // commonが無い場合には、このシートの処理をスキップします。
                //System.out.println("BlancoRestPhpXmlSourceFile#processTelegramProcess !!! NO COMMON !!!");
                continue;
            }
            //プロパティブロックが見つかった場合
            blnFoundElementCommon = true;

            final String name = BlancoXmlBindingUtil.getTextContent(
                    elementCommon, "name");

            if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
                // nameが空の場合には処理をスキップします。
                System.out.println("BlancoRestPhpXmlSourceFile#processTelegramProcess !!! NO NAME !!!");
                continue;
            }

            System.out.println("BlancoRestPhpXmlSourceFile#processTelegramProcess name = " + name);

            // 継承情報を取得します
            final BlancoXmlElement elementExtends = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle.getMeta2xmlProcessExtends());

            // 電文処理には一覧情報はありません

            // シートから詳細な情報を取得します。
            final BlancoRestPhpTelegramProcess structure = parseProcessSheet(elementCommon, elementExtends);
//            System.out.println("requestId = " + structure.getRequestId());

            if (structure != null) {
                // メタ情報の解析結果をもとにソースコード自動生成を実行します。
                process(structure, argListTelegrams, argDirectoryTarget);
            }

        }
        if(!blnFoundElementCommon){
            System.out.println("BlancoRestPhpXmlSourceFile#processTelegramProcess !!! NOT ANY COMMON !!!");
        }
    }

    private BlancoRestPhpTelegramProcess parseProcessSheet(final BlancoXmlElement argElementCommon, final BlancoXmlElement argElementExtends) {

        final BlancoRestPhpTelegramProcess structure = new BlancoRestPhpTelegramProcess();
        structure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));

        String namespace = BlancoXmlBindingUtil.getTextContent(argElementCommon,"package");
        if (namespace == null) {
            namespace = "";
        }
        structure.setPackage(namespace);

        if (BlancoStringUtil.null2Blank(structure.getPackage()).trim()
                .length() == 0) {
            throw new IllegalArgumentException(fBundle
                    .getXml2sourceFileErr001(structure.getName()));
        }

        if (BlancoXmlBindingUtil
                .getTextContent(argElementCommon, "description") != null) {
            structure.setDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "description"));
        }
//        System.out.println("### requestId = " + BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramRequestId"));
        structure.setRequestId(BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramRequestId"));
        structure.setResponseId(BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramResponseId"));
        structure.setLocation(BlancoXmlBindingUtil.getTextContent(argElementCommon, "location"));

        structure.setNamespace(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "namespace"));

        String strNoAuthenticationRequired = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "noAuthentication");
        // System.out.println("#### noAuth = " + strNoAuthenticationRequired);
        structure.setNoAuthentication("YES".equalsIgnoreCase(strNoAuthenticationRequired));

        String strSlaveSearchRequired = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "slaveSearch");
        // System.out.println("#### slaveSearch = " + strSlaveSearchRequired);
        structure.setSlaveSearch("YES".equalsIgnoreCase(strSlaveSearchRequired));

        // 継承情報を取得します。
        String superClass = BlancoRestPhpConstants.BASE_CLASS;
        if (argElementExtends != null) {
            superClass = BlancoXmlBindingUtil.getTextContent(argElementExtends, "superClass");
            if (superClass == null) {
                superClass = BlancoRestPhpConstants.BASE_CLASS;
            } else {
                String extendsNamespace = BlancoXmlBindingUtil.getTextContent(argElementExtends, "package");
                if (extendsNamespace != null) {
                    superClass = extendsNamespace + "\\" + superClass;
                }
            }
        }
        structure.setSuperClass(superClass);

        return structure;
    }

    private void processTelegram(final File argDirectoryTarget, BlancoXmlElement elementRoot, List<BlancoRestPhpTelegram> argListTelegrams) {

        //Commonプロパティブロックが見つかったかどうか
        Boolean blnFoundElementCommon = false;

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(elementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // 共通情報を取得します。
            final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle
                            .getMeta2xmlTelegramCommon());
            if (elementCommon == null) {
                // commonが無い場合には、このシートの処理をスキップします。
                //System.out.println("BlancoRestPhpXmlSourceFile#process !!! NO COMMON !!!");
                continue;
            }
            //プロパティブロックが見つかった場合
            blnFoundElementCommon = true;

            final String name = BlancoXmlBindingUtil.getTextContent(
                    elementCommon, "name");

            if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
                // nameが空の場合には処理をスキップします。
                System.out.println("BlancoRestPhpXmlSourceFile#process !!! NO NAME !!!");
                continue;
            }

            System.out.println("BlancoRestPhpXmlSourceFile#process name = " + name);

            // 継承情報を取得します
            final BlancoXmlElement elementExtends = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle.getMeta2xmlTelegramExtends());



            // 一覧情報を取得します。
            final BlancoXmlElement elementList = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle.getMeta2xmlTeregramList());

            // シートから詳細な情報を取得します。
            final BlancoRestPhpTelegram processTelegram = parseTelegramSheet(
                    elementCommon, elementExtends, elementList);

            if (processTelegram != null) {
                // メタ情報の解析結果をもとにソースコード自動生成を実行します。
                process(processTelegram, argDirectoryTarget);
                argListTelegrams.add(processTelegram);
            }
        }
        if(!blnFoundElementCommon){
            System.out.println("BlancoRestPhpXmlSourceFile#process !!! NOT ANY COMMON !!!");
        }
    }

    /**
     * sheetエレメントを展開します。
     * 
     * @param argElementCommon
     *            現在処理しているCommonノード。
     * @param argElementList
     *            現在処理しているListノード。
     * @return 収集されたメタ情報構造データ。
     */
    private BlancoRestPhpTelegram parseTelegramSheet(
            final BlancoXmlElement argElementCommon,
            final BlancoXmlElement argElementExtends,
            final BlancoXmlElement argElementList) {

        final BlancoRestPhpTelegram processTelegram = new BlancoRestPhpTelegram();
        processTelegram.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        String namespace = BlancoXmlBindingUtil.getTextContent(argElementCommon,"package");
        if (namespace == null) {
            namespace = "";
        }
        processTelegram.setPackage(namespace);

        if (BlancoStringUtil.null2Blank(processTelegram.getPackage()).trim()
                .length() == 0) {
            throw new IllegalArgumentException(fBundle
                    .getXml2sourceFileErr001(processTelegram.getName()));
        }

        if (BlancoXmlBindingUtil
                .getTextContent(argElementCommon, "description") != null) {
            processTelegram.setDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "description"));
        }

        processTelegram.setNamespace(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "namespace"));

        processTelegram.setTelegramType(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "type"));

        // 継承情報を取得します。

        if (argElementExtends != null) {
            String superClass = BlancoXmlBindingUtil.getTextContent(argElementExtends, "superClass");
            if (superClass == null) {
                if (BlancoRestPhpConstants.TELEGRAM_TYPE_INPUT.equalsIgnoreCase(processTelegram.getTelegramType())) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileRequestidNosuperclass(processTelegram.getName()));
                } else {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileResponseidNosuperclass(processTelegram.getName()));
                }
            } else {
                String extendsNamespace = BlancoXmlBindingUtil.getTextContent(argElementExtends, "package");
                if (extendsNamespace != null) {
//                    superClass = extendsNamespace + "\\" + superClass;
                    processTelegram.setPackageSuperClass(extendsNamespace);
                }
            }
            processTelegram.setTelegramSuperClass(superClass);
        } else {
            processTelegram.setTelegramSuperClass(BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "superClass"));
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
                System.out.println("BlancoRestPhpXml2SourceFile#parseTelegramSheet: NO FIELD !!!");
                continue;
            }

            final BlancoXmlElement elementField = (BlancoXmlElement) nodeField;
            BlancoRestPhpTelegramField field = new BlancoRestPhpTelegramField();
            field
                    .setNo(BlancoXmlBindingUtil.getTextContent(elementField,
                            "no"));

            field.setName(BlancoXmlBindingUtil.getTextContent(elementField,
                    "fieldName"));
            if (BlancoStringUtil.null2Blank(field.getName()).length() == 0) {
                continue;
            }

//            System.out.println("BlancoRestPhpXml2SourceFile#parseTelegramSheet: name = " + field.getName());

            // 既に同じ内容が登録されていないかどうかのチェック。
            for (int indexPast = 0; indexPast < processTelegram.getListField()
                    .size(); indexPast++) {
                final BlancoRestPhpTelegramField fieldPast = processTelegram
                        .getListField().get(indexPast);
                if (fieldPast.getName().equals(field.getName())) {
                    throw new IllegalArgumentException(
                            fBundle.getXml2sourceFileErr003(processTelegram
                                    .getName(), field.getName()));
                }
            }

            field.setFieldType(BlancoXmlBindingUtil.getTextContent(elementField,
                    "fieldType"));
            if (BlancoStringUtil.null2Blank(field.getFieldType()).length() == 0) {
                // ここで異常終了。
                continue;
            }

            field.setFieldGeneric(BlancoXmlBindingUtil.getTextContent(elementField,
                    "fieldGeneric"));
            if (BlancoStringUtil.null2Blank(field.getFieldGeneric()).length() != 0) {
                /* debug */
                System.out.println("/* tueda */ generic : " + field.getFieldGeneric() + ", type : " + field.getFieldType());
            }

            field.setDescription(BlancoXmlBindingUtil.getTextContent(
                    elementField, "description"));

            String strFieldRequired = BlancoXmlBindingUtil.getTextContent(
                    elementField, "fieldRequired");
            field.setFieldRequired("YES".equalsIgnoreCase(strFieldRequired));

            field.setDefault(BlancoXmlBindingUtil.getTextContent(
                    elementField, "default"));

            try {
                String strMinLength = BlancoXmlBindingUtil.getTextContent(
                        elementField, "minLength");
                field.setMinLength(Integer.parseInt(strMinLength));

                String strMaxLength = BlancoXmlBindingUtil.getTextContent(
                        elementField, "maxLength");
                field.setMaxLength(Integer.parseInt(strMaxLength));
            } catch (NumberFormatException e) {
                // 値がセットされていなかったり数値でなかった場合は無視
            }

            field.setMinInclusive(BlancoXmlBindingUtil.getTextContent(
                    elementField, "minInclusive"));
            field.setMaxInclusive(BlancoXmlBindingUtil.getTextContent(
                    elementField, "maxInclusive"));

            field.setPattern(BlancoXmlBindingUtil.getTextContent(
                    elementField, "pattern"));

            field.setFieldBiko(BlancoXmlBindingUtil.getTextContent(
                    elementField, "fieldBiko"));

            processTelegram.getListField().add(field);
        }

        return processTelegram;
    }

    /**
     * 収集された情報を元に、電文処理のソースコードを自動生成します。
     *
     * @param argStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void process(
            final BlancoRestPhpTelegramProcess argStructure,
            final List<BlancoRestPhpTelegram> argListTelegrams,
            final File argDirectoryTarget) {

        // 従来と互換性を持たせるため、/mainサブフォルダに出力します。
        final File fileBlancoMain = new File(argDirectoryTarget
                .getAbsolutePath()
                + "/main");

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(argStructure
                .getPackage(), "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(BlancoRestPhpConstants.PREFIX_ABSTRACT + argStructure.getName(),
                BlancoStringUtil.null2Blank(argStructure
                        .getDescription()));
        // ApiBase クラスを継承
        BlancoCgType fCgType = new BlancoCgType();
        fCgType.setName(argStructure.getSuperClass());
        fCgClass.setExtendClassList(new ArrayList<BlancoCgType>());
        fCgClass.getExtendClassList().add(fCgType);

        // abstrac フラグをセット
        fCgClass.setAbstract(true);

        fCgSourceFile.getClassList().add(fCgClass);

        if (argStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argStructure
                    .getDescription());
        }

        // API実装クラスで実装させる abstract method の定義
        createAbstractMethod(argStructure, argListTelegrams);


//        System.out.println("requestId = " + argStructure.getRequestId());
        // base class からの abstract method の実装
        createExecuteMethod(argStructure, argListTelegrams);

        // isAuthenticationRequired メソッドの上書き
        overrideAuthenticationRequired(argStructure);

        // RequestId 名を取得する メソッド
        createRequestIdMethod(argStructure, argListTelegrams);

        // ResponseId 名を取得する メソッド
        createResponseIdMethod(argStructure, argListTelegrams);

        // isSlaveSearchRequired メソッドの上書き
        overrideSlaveSearchRequired(argStructure);

        // required 文を出力しない ... 将来的には xls で指定するように？
        fCgSourceFile.setIsImport(false);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }

    private void createAbstractMethod(BlancoRestPhpTelegramProcess argStructure, List<BlancoRestPhpTelegram>  argListTelegrams) {

        // Initializer の定義
//        final BlancoCgMethod cgInitializerMethod = fCgFactory.createMethod(
//                BlancoRestPhpConstants.API_INITIALIZER_METHOD, fBundle.getXml2sourceFileInitializerDescription());
//        fCgClass.getMethodList().add(cgInitializerMethod);
//        cgInitializerMethod.setAccess("protected");
//        cgInitializerMethod.setAbstract(true);
        // ApiBase で固定的に定義

        // Processor の定義
        final BlancoCgMethod cgProcessorMethod = fCgFactory.createMethod(
                BlancoRestPhpConstants.API_PROCESS_METHOD, fBundle.getXml2sourceFileProcessorDescription());
        fCgClass.getMethodList().add(cgProcessorMethod);
        cgProcessorMethod.setAccess("protected");
        cgProcessorMethod.setAbstract(true);

        String requestId = argStructure.getRequestId();
        String requestIdPackage = null;
        String responseId = argStructure.getResponseId();
        String responseIdPackage = null;

        for (BlancoRestPhpTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Input".equals(telegram.getTelegramType())) {
                String anoRequestId = telegram.getName();
                if (anoRequestId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileRequestidNosuperclass(requestId));
                }
                requestId = anoRequestId;
                requestIdPackage = telegram.getPackage();
            }
            if ("Output".equals(telegram.getTelegramType())) {
                String anoResponseId = telegram.getName();
                if (anoResponseId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileResponseidNosuperclass(responseId));
                }
                responseId = anoResponseId;
                responseIdPackage = telegram.getPackage();
            }
        }

        String fullRequestId = requestIdPackage == null ? requestId : requestIdPackage + "\\" + requestId;
        String fullResponseId = responseIdPackage == null ? responseId : responseIdPackage + "\\" + responseId;

        cgProcessorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestId, fullRequestId,
                        fBundle.getXml2sourceFileProsessorArgLangdoc()));

        cgProcessorMethod.setReturn(fCgFactory.createReturn(fullResponseId,
                fBundle.getXml2sourceFileProsessorReturnLangdoc()));

    }

    private void createExecuteMethod(BlancoRestPhpTelegramProcess argStructure, List<BlancoRestPhpTelegram>  argListTelegrams) {
        final BlancoCgMethod cgExecutorMethod = fCgFactory.createMethod(
                BlancoRestPhpConstants.BASE_EXECUTOR_METHOD, fBundle.getXml2sourceFileExecutorDescription());
        fCgClass.getMethodList().add(cgExecutorMethod);
        cgExecutorMethod.setAccess("protected");

        /*
         * 型チェックを通す為にSuperClassがある場合はそれを使います
         */
        String requestId = argStructure.getRequestId();
        String requestIdPackage = null;
        String responseId = argStructure.getResponseId();
        String responseIdPackage = null;
        for (BlancoRestPhpTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Input".equals(telegram.getTelegramType())) {
                String anoRequestId = telegram.getTelegramSuperClass();
                if (anoRequestId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileRequestidNosuperclass(requestId));
                }
                requestId = anoRequestId;
                requestIdPackage = telegram.getPackageSuperClass();
            }
            if ("Output".equals(telegram.getTelegramType())) {
                String anoResponseId = telegram.getTelegramSuperClass();
                if (anoResponseId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileResponseidNosuperclass(responseId));
                }
                responseId = anoResponseId;
                responseIdPackage = telegram.getPackageSuperClass();
            }
        }

        String fullReqeustId = requestIdPackage == null ? requestId : requestIdPackage + "\\" + requestId;
        String fullResponseId = responseIdPackage == null ? responseId : responseIdPackage + "\\" + responseId;

        //        System.out.println("### requestId = " + requestId);
        cgExecutorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestId, fullReqeustId,
                        fBundle
                                .getXml2sourceFileExecutorArgLangdoc()));

        cgExecutorMethod.setReturn(fCgFactory.createReturn(fullResponseId,
                fBundle.getXml2sourceFileExecutorReturnLangdoc()));

        // メソッドの実装
        final List<String> listLine = cgExecutorMethod.getLineList();

        listLine.add(
                BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseId + " = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "this->" + BlancoRestPhpConstants.API_PROCESS_METHOD
                        + "( " + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg" + requestId + " )"
                        + BlancoCgLineUtil.getTerminator(fTargetLang));

        listLine.add("\n");
        listLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseId
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void overrideAuthenticationRequired(BlancoRestPhpTelegramProcess argStructure) {
        String methodName = BlancoRestPhpConstants.API_AUTHENTICATION_REQUIRED;

        final BlancoCgMethod cgAuthenticationRequiredMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileAuthflagDescription());
        fCgClass.getMethodList().add(cgAuthenticationRequiredMethod);
        cgAuthenticationRequiredMethod.setAccess("protected");

        // メソッドの実装
        final List<String> listLine = cgAuthenticationRequiredMethod.getLineList();

        String retval = "true";
        if (argStructure.getNoAuthentication()) {
            retval = "false";
        }

        listLine.add("return " + retval
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void createRequestIdMethod(BlancoRestPhpTelegramProcess argStructure, List<BlancoRestPhpTelegram>  argListTelegrams) {
        String methodName = BlancoRestPhpConstants.API_REQUESTID_METHOD;

        final BlancoCgMethod cgResponseIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileRequestidDesctiption());
        fCgClass.getMethodList().add(cgResponseIdMethod);
        cgResponseIdMethod.setAccess("protected");

        // メソッドの実装
        final List<String> listLine = cgResponseIdMethod.getLineList();

        String requestId = argStructure.getRequestId();
        String requestIdPackage = null;

        for (BlancoRestPhpTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Input".equals(telegram.getTelegramType())) {
                String anoRequestId = telegram.getName();
                if (anoRequestId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileRequestidNosuperclass(requestId));
                }
                requestId = anoRequestId;
                requestIdPackage = telegram.getPackage();
            }
        }

        String fullReqeustId = requestIdPackage == null ? requestId : requestIdPackage + "\\" + requestId;

        listLine.add("return " + "\"" + fullReqeustId + "\""
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void createResponseIdMethod(BlancoRestPhpTelegramProcess argStructure, List<BlancoRestPhpTelegram>  argListTelegrams) {
        String methodName = BlancoRestPhpConstants.API_RESPONSE_METHOD;

        final BlancoCgMethod cgResponseIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileResponseidDescription());
        fCgClass.getMethodList().add(cgResponseIdMethod);
        cgResponseIdMethod.setAccess("protected");

        // メソッドの実装
        final List<String> listLine = cgResponseIdMethod.getLineList();

        String responseId = argStructure.getResponseId();
        String responseIdPackage = null;

        for (BlancoRestPhpTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Output".equals(telegram.getTelegramType())) {
                String anoResponseId = telegram.getName();
                if (anoResponseId == null) {
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileResponseidNosuperclass(responseId));
                }
                responseId = anoResponseId;
                responseIdPackage = telegram.getPackage();
            }
        }

        String fullResponseId = responseIdPackage == null ? responseId : responseIdPackage + "\\" + responseId;

        listLine.add("return " + "\"" + fullResponseId + "\""
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void overrideSlaveSearchRequired(BlancoRestPhpTelegramProcess argStructure) {

        String methodName = BlancoRestPhpConstants.API_SLAVESEARCH_REQUIRED;

        final BlancoCgMethod cgSlaveSearchRequiredMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileAuthflagDescription());
        fCgClass.getMethodList().add(cgSlaveSearchRequiredMethod);
        cgSlaveSearchRequiredMethod.setAccess("protected");

        // メソッドの実装
        final List<String> listLine = cgSlaveSearchRequiredMethod.getLineList();

        String retval = "false";
        if (argStructure.getSlaveSearch()) {
            retval = "true";
        }

        listLine.add("return " + retval
                + BlancoCgLineUtil.getTerminator(fTargetLang));

    }

    /**
     * 収集された情報を元に、ソースコードを自動生成します。
     * 
     * @param argStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void process(
            final BlancoRestPhpTelegram argStructure,
            final File argDirectoryTarget) {

        // 従来と互換性を持たせるため、/mainサブフォルダに出力します。
        final File fileBlancoMain = new File(argDirectoryTarget
                .getAbsolutePath()
                + "/main");

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(argStructure
                .getPackage(), "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(argStructure.getName(),
                BlancoStringUtil.null2Blank(argStructure
                        .getDescription()));

        // ApiTelegram クラスを継承
        String telegramBase = argStructure.getTelegramSuperClass();
        String telegramBasePackage = argStructure.getPackageSuperClass();
        if (telegramBase != null) {
            BlancoCgType fCgType = new BlancoCgType();
            if (telegramBasePackage != null) {
                telegramBase = telegramBasePackage + "\\" + telegramBase;
            }
            fCgType.setName(telegramBase);

            fCgClass.setExtendClassList(new ArrayList<BlancoCgType>());
            fCgClass.getExtendClassList().add(fCgType);

        }

        fCgSourceFile.getClassList().add(fCgClass);

        if (argStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argStructure
                    .getDescription());
        }

        expandValueObject(argStructure);

        // required 文を出力しない ... 将来的には xls で指定するように？
        fCgSourceFile.setIsImport(false);

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
            final BlancoRestPhpTelegram argProcessStructure) {

        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestPhpTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            expandField(argProcessStructure, fieldLook);

            expandMethodSet(argProcessStructure, fieldLook);

            expandMethodGet(argProcessStructure, fieldLook);

            expandMethodType(argProcessStructure, fieldLook);

            expandMethodGeneric(argProcessStructure, fieldLook);
        }

        //バリデート関係
        expandMethodValidate(argProcessStructure);

        expandMethodToString(argProcessStructure);
    }

    /**
     * バリデートフィールドを展開します。
     *
     */
    private void expandMethodValidate(
            final BlancoRestPhpTelegram argProcessStructure) {

        String strMinLength = "";
        String strMaxLength = "";
        String strMinInclusive = "";
        String strMaxInclusive = "";
        String strPattern = "";
        String strFieldRequired = "";

        boolean isMinLengthLoop = false;
        boolean isMaxLengthLoop = false;
        boolean isMinInclusiveLoop = false;
        boolean isMaxInclusiveLoop = false;
        boolean isPatternLoop = false;
        boolean isFieldRequiredLoop = false;

        String strIndentBlank = "        ";

        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestPhpTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            String fieldName = fieldLook.getName();
            if (fNameAdjust) {
                fieldName = BlancoNameAdjuster.toParameterName(fieldName);

            }

            //System.out.println("ooq  name = " +  fieldName);

            //Min長 後に出力する為、保持します。
            if (fieldLook.getMinLength() != null) {
                if (isMinLengthLoop) {
                    strMinLength += ",\n" + strIndentBlank;
                }
                strMinLength += "'" + fieldName + "' => '" + fieldLook.getMinLength() + "'";
                isMinLengthLoop = true;
            }

            //Max長 後に出力する為、保持します。
            if (fieldLook.getMaxLength() != null) {
                if (isMaxLengthLoop) {
                    strMaxLength += ",\n" + strIndentBlank;
                }
                strMaxLength += "'" + fieldName + "' => '" + fieldLook.getMaxLength() + "'";
                isMaxLengthLoop = true;
            }

            //Min値 後に出力する為、保持します。
            if (fieldLook.getMinInclusive() != null) {
                if (isMinInclusiveLoop) {
                    strMinInclusive += ",\n" + strIndentBlank;
                }
                strMinInclusive += "'" + fieldName + "' => " + fieldLook.getMinInclusive();
                isMinInclusiveLoop = true;
            }

            //Max値 後に出力する為、保持します。
            if (fieldLook.getMaxInclusive() != null) {
                if (isMaxInclusiveLoop) {
                    strMaxInclusive += ",\n" + strIndentBlank;
                }
                strMaxInclusive += "'" + fieldName + "' => " + fieldLook.getMaxInclusive();
                isMaxInclusiveLoop = true;
            }

            //正規表現 後に出力する為、保持します。
            if (fieldLook.getPattern() != null) {
                if (isPatternLoop) {
                    strPattern += ",\n" + strIndentBlank;
                }
                strPattern += "'" + fieldName + "' => '" + fieldLook.getPattern()  + "'";
                isPatternLoop = true;
            }

            //必須 後に出力する為、保持します。
            if (fieldLook.getFieldRequired() != null) {
                if (fieldLook.getFieldRequired() == true){
                    if (isFieldRequiredLoop) {
                        strFieldRequired += ",\n" + strIndentBlank;
                    }
                    strFieldRequired += "'" + fieldName + "' => 'YES'";
                    isFieldRequiredLoop = true;
                }
            }

        }

        if(strMinLength != ""){
            createValidateMethod(
                    strMinLength,
                    "arrayMinLength",
                    fBundle.getXml2sourceFileVgetarrayminlengthDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYMINLENGTH_METHOD,
                    fBundle.getXml2sourceFileVgetarrayminlengthDescription()
            );
        }

        if(strMaxLength != ""){
            createValidateMethod(
                    strMaxLength,
                    "arrayMaxLength",
                    fBundle.getXml2sourceFileVgetarraymaxlengthDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYMAXLENGTH_METHOD,
                    fBundle.getXml2sourceFileVgetarraymaxlengthDescription()
            );
        }

        if(strMinInclusive != ""){
            createValidateMethod(
                    strMinInclusive,
                    "arrayMinInclusive",
                    fBundle.getXml2sourceFileVgetarraymininclusiveDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYMININCLUSIVE_METHOD,
                    fBundle.getXml2sourceFileVgetarraymininclusiveDescription()
            );
        }

        if(strMaxInclusive != ""){
            createValidateMethod(
                    strMaxInclusive,
                    "arrayMaxInclusive",
                    fBundle.getXml2sourceFileVgetarraymaxinclusiveDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYMAXINCLUSIVE_METHOD,
                    fBundle.getXml2sourceFileVgetarraymaxinclusiveDescription()
            );
        }

        if(strPattern != ""){
            createValidateMethod(
                    strPattern,
                    "arrayPattern",
                    fBundle.getXml2sourceFileVgetarraypattarnDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYPATTARN_METHOD,
                    fBundle.getXml2sourceFileVgetarraypattarnDescription()
            );
        }

        if(strFieldRequired != ""){
            createValidateMethod(
                    strFieldRequired,
                    "arrayFieldRequired",
                    fBundle.getXml2sourceFileVgetarrayfieldrequiredDescription(),
                    BlancoRestPhpConstants.API_VGETARRAYFIELDREQUIRED_METHOD,
                    fBundle.getXml2sourceFileVgetarrayfieldrequiredDescription()
            );
        }

    }


    /**
     * バリデートフィールドを展開します。
     *
     * @param strStackValue
     * @param argName
     * @param argFieldDescription
     * @param methodNam
     * @param strStackValue
     */
    private void createValidateMethod(
            String strStackValue,
            String argName,
            String argFieldDescription,
            String methodNam,
            String argMethodDescription) {

        String strIndentBlank = "        ";

        //プロパティ部分
        final BlancoCgField cgField = fCgFactory.createField(argName ,
                "array", "");
        fCgClass.getFieldList().add(cgField);
        cgField.setAccess("protected");

        cgField.getLangDoc().getDescriptionList().add(
                argFieldDescription);

        cgField.setDefault("array(\n" + strIndentBlank + strStackValue +")");

        //メソッド部分
        final BlancoCgMethod cgMethod = fCgFactory.createMethod(methodNam,argMethodDescription
                );
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileGetLangdoc02("array"));

        cgMethod.setReturn(fCgFactory.createReturn("array", fBundle.getXml2sourceFileGetReturnLangdoc(methodNam)));

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "this->" + argName
                        + BlancoCgLineUtil.getTerminator(fTargetLang));

    }

    /**
     * フィールドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandField(
            final BlancoRestPhpTelegram argProcessStructure,
            final BlancoRestPhpTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgField cgField = fCgFactory.createField("f" + fieldName,
                fieldLook.getFieldType(), "");
        fCgClass.getFieldList().add(cgField);
        cgField.setAccess("private");

        cgField.setDescription(fBundle.getXml2sourceFileFieldName(fieldLook
                .getName()));
        cgField.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileFieldType(fieldLook.getFieldType()));
        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgField.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        if (fieldLook.getDefault() != null) {
            if (fieldLook.getFieldType().equals("string")) {
                // クオートを付与します。
                cgField.setDefault(BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang)
                        + BlancoJavaSourceUtil
                        .escapeStringAsJavaSource(fieldLook
                                .getDefault())
                        + BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang));
            } else if (fieldLook.getFieldType().equals("boolean")
                    || fieldLook.getFieldType().equals("integer")
                    || fieldLook.getFieldType().equals("float")
                    || fieldLook.getFieldType().equals("double")) {
                cgField.setDefault(fieldLook.getDefault());
            } else {
                throw new IllegalArgumentException(fBundle
                        .getXml2sourceFileErr006(argProcessStructure.getName(),
                                fieldLook.getName(), fieldLook.getDefault(),
                                fieldLook.getFieldType()));
            }
        }

    }

    /**
     * setメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodSet(
            final BlancoRestPhpTelegram argProcessStructure,
            final BlancoRestPhpTelegramField fieldLook) {
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
                fBundle.getXml2sourceFileSetLangdoc02(fieldLook.getFieldType()));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        cgMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + fieldName, fieldLook
                        .getFieldType(), fBundle
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
            final BlancoRestPhpTelegram argProcessStructure,
            final BlancoRestPhpTelegramField fieldLook) {
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
                fBundle.getXml2sourceFileGetLangdoc02(fieldLook.getFieldType()));

        cgMethod.setReturn(fCgFactory.createReturn(fieldLook.getFieldType(), fBundle
                .getXml2sourceFileGetReturnLangdoc(fieldLook.getName())));

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
     * typeメソッドを展開します
     *
     * @param argProcessStructure
     * @param fieldLook
     */
    private void expandMethodType(
            final BlancoRestPhpTelegram argProcessStructure,
            final BlancoRestPhpTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("type"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.setStatic(true);

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileTypeLangdoc02(fieldLook.getFieldType()));

        cgMethod.setReturn(fCgFactory.createReturn(fieldLook.getFieldType(), fBundle
                .getXml2sourceFileTypeReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + "\"" + fieldLook.getFieldType() + "\""
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void expandMethodGeneric(
            final BlancoRestPhpTelegram argProcessStructure,
            final BlancoRestPhpTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        String fieldGeneric = fieldLook.getFieldGeneric();

        if (BlancoStringUtil.null2Blank(fieldGeneric).length() == 0) {
            return;
        }

        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("generic"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.setStatic(true);

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileTypeLangdoc02(fieldGeneric));

        cgMethod.setReturn(fCgFactory.createReturn(fieldGeneric, fBundle
                .getXml2sourceFileTypeReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + "\"" + fieldLook.getFieldGeneric() + "\""
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * toStringメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodToString(
            final BlancoRestPhpTelegram argProcessStructure) {
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
            final BlancoRestPhpTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            String fieldName = fieldLook.getName();
            if (fNameAdjust) {
                fieldName = BlancoNameAdjuster.toClassName(fieldName);
            }

            if (fieldLook.getFieldType().equals("array") == false) {
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
                if (fieldLook.getFieldType().equals("string")) {
                    strLine += BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this->f" + fieldName;
                } else if (fieldLook.getFieldType().equals("boolean")) {
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
