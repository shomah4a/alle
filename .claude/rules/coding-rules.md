## collectionの扱い

java.util のコレクションクラス類は使わずに Eclipse Collections を使うこと。
java.util のコレクションはmutabilityが型レベルでチェックできないので使用してはいけない。

クラスのフィールドやメソッドのローカル変数では Mutable Collection を使うこと。
返り値などでは Immutable なインターフェイスを返す。

例えばメソッド内での返り値の組み立てでは MutableList を使う。
返り値では ListIterable を使う。

インターフェイスや返り値に Mutable なコレクションが出てきた場合は設計を疑う必要があるので、ユーザーに確認すること。
