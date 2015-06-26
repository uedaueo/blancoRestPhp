blancoSOAP はSOAPに関連するクラスを集めたものです。
 1.電文処理定義書・電文定義書といった様式から各種ファイルを自動生成します。
   (1)電文処理定義書・電文定義書から WSDLおよびXML Schema (xsd)を自動生成します。
   (2)xsdを入力として Java言語用のValueObjectソースコードを自動生成します。
 2.Apache AntタスクまたはEclipseプラグインの形式で配布されています。

 *..NET Framework版は 現在は .NET Framework 1.1のみの対応となります。

[開発者]
 1.山本耕司 (Y-moto) : リリース判定担当。様式担当。
 2.伊賀敏樹 (Tosiki Iga / いがぴょん): 開発および維持メンテ担当
 3.小堀陽平 : 試験およびリリース判定担当

[ライセンス]
 1.blancoSOAP は ライセンス として GNU Lesser General Public License を採用しています。

[依存するライブラリ]
blancoSOAPは下記のライブラリを利用しています。
※各オープンソース・プロダクトの提供者に感謝します。
 1.JExcelApi - Java Excel API - A Java API to read, write and modify Excel spreadsheets
     http://jexcelapi.sourceforge.net/
     http://sourceforge.net/projects/jexcelapi/
     http://www.andykhan.com/jexcelapi/ 
   概要: JavaからExcelブック形式を読み書きするためのライブラリです。
   ライセンス: GNU Lesser General Public License
 2.blancoCodeGenerator
   概要: ソースコード生成ライブラリ
   ライセンス: GNU Lesser General Public License
 3.blancoCommons
   概要: blanco Framework共通ライブラリ
         メタ情報ファイルを読み込む際に利用しています。
   ライセンス: GNU Lesser General Public License
