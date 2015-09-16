<?php
/**
 * Created by IntelliJ IDEA.
 * User: tueda
 * Date: 15/07/05
 * Time: 15:33
 */

namespace blanco\php;


abstract class ApiBase {

    protected abstract function execute();

    public static function call() {

    }

    /**
     * なんでもcast関数
     *
     * @param $obj
     * @param $toClass
     * @return bool|mixed
     */
    protected function cast($obj, $toClass) {
        if (!class_exists($toClass)) {
            return false;
        }
        $length = strlen($toClass);
        $objIn  = serialize($obj);
        $objOut = '';
        if (preg_match('/\AO:\d+:\".*?\":(.*?)\z/', $objIn, $matches)) {
            $objOut = sprintf('O:%d:"%s":%s', $length, $toClass, $matches[1]);
        }
        return unserialize($objOut);
    }
}