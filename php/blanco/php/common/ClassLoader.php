<?php

/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/09/14
 * Time: 1:46
 */
class ClassLoader
{
    // class ファイルがあるディレクトリのリスト
    private static $dirs;

    public static function _autoLoad($className) {
        foreach (self::$dirs as $directory) {
            $fileName = "{$directory}/{$className}.php";

            if (is_file($fileName)) {
                require $fileName;
                return true;
            }
        }
        return false;
    }

    public static function addPath($argPathList) {
        self::$dirs = $argPathList;
    }

    public static function dumpPath() {
        var_dump(self::$dirs);
    }
}
