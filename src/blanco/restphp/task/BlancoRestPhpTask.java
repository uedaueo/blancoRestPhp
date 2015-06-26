/*
 * blanco Framework
 * Copyright (C) 2004-2005 IGA Tosiki
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.soap.task;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import blanco.soap.BlancoSoapWsdl2CsClass;

/**
 * BlancoCsvのAntタスクです。
 * 
 * @author IGA Tosiki
 */
public class BlancoSoapDotNetWsdl2TelegramProcessTask extends
        AbstractBlancoRestPhpTask {

    /**
     * AntTaskの処理を実行します。
     * 
     * このメソッドは 抽象親クラスであるAbstractBlancoRestPhpTaskクラスから呼び出されます。
     * 
     * @throws IllegalArgumentException
     *             各種入力値例外の場合に発生します。
     */
    protected void process() throws IllegalArgumentException {
        try {
            final File fileMetadir = new File(getMetadir());
            if (fileMetadir.exists() == false) {
                throw new IllegalArgumentException("メタディレクトリ[" + getMetadir()
                        + "]が存在しません。");
            }

            final File[] fileMeta = fileMetadir.listFiles();
            for (int index = 0; index < fileMeta.length; index++) {
                if (fileMeta[index].getName().endsWith(".wsdl") == false) {
                    continue;
                }
                try {
                    new BlancoSoapWsdl2CsClass().process(fileMeta[index],
                            new File(getTargetdir()), getServer()
                                    .equals("true"));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("ファイル["
                            + fileMeta[index] + "]の処理のうえで例外が発生しました。"
                            + ex.toString());
                }
            }
        } catch (TransformerException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (IOException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
}
