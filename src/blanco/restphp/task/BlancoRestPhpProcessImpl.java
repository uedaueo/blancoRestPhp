/*
 * blanco Framework
 * Copyright (C) 2004-2009 IGA Tosiki
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.restphp.task;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import blanco.restphp.BlancoRestPhpConstants;
import blanco.restphp.BlancoRestPhpXml2SourceFile;
import blanco.restphp.resourcebundle.BlancoRestPhpResourceBundle;

import blanco.restphp.BlancoRestPhpMeta2Xml;
//import blanco.restphp.BlancoRestPhpXml2SourceFile;
import blanco.restphp.task.valueobject.BlancoRestPhpProcessInput;

public class BlancoRestPhpProcessImpl implements
        BlancoRestPhpProcess {
    /**
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestPhpResourceBundle fBundle = new BlancoRestPhpResourceBundle();

    /**
     * {@inheritDoc}
     */
    public int execute(final BlancoRestPhpProcessInput input) {
        System.out.println("- " + BlancoRestPhpConstants.PRODUCT_NAME
                + " (" + BlancoRestPhpConstants.VERSION + ")");

        try {
            final File fileMetadir = new File(input.getMetadir());
            if (fileMetadir.exists() == false) {
                throw new IllegalArgumentException(fBundle
                        .getAnttaskErr001(input.getMetadir()));
            }

            // テンポラリディレクトリを作成。
            new File(input.getTmpdir()
                    + BlancoRestPhpConstants.TARGET_SUBDIRECTORY)
                    .mkdirs();

            // 指定されたメタディレクトリを処理します。
            new BlancoRestPhpMeta2Xml()
                    .processDirectory(fileMetadir, input.getTmpdir()
                            + BlancoRestPhpConstants.TARGET_SUBDIRECTORY);

            // XML化された中間ファイルからソースコードを生成
            final File[] fileMeta2 = new File(input.getTmpdir()
                    + BlancoRestPhpConstants.TARGET_SUBDIRECTORY)
                    .listFiles();
            for (int index = 0; index < fileMeta2.length; index++) {
                if (fileMeta2[index].getName().endsWith(".xml") == false) {
                    continue;
                }

                final BlancoRestPhpXml2SourceFile xml2source = new BlancoRestPhpXml2SourceFile();
                xml2source.setEncoding(input.getEncoding());
                xml2source.process(fileMeta2[index], "true".equals(input
                        .getNameAdjust()), new File(input.getTargetdir()));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.toString());
        } catch (TransformerException ex) {
            throw new IllegalArgumentException(ex.toString());
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            throw ex;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean progress(final String argProgressMessage) {
        System.out.println(argProgressMessage);
        return false;
    }
}
