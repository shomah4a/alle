package io.github.shomah4a.alle.core.mode.modes.git;

import java.time.Instant;

/**
 * git log の 1 コミット分の情報。
 *
 * @param shortHash git log の %h に対応する短縮コミットハッシュ
 * @param commitTime コミット時刻 (git の %aI から変換した Instant)
 * @param author 作者名 (git の %an)
 * @param subject コミットメッセージ 1 行目 (git の %s)
 * @param body コミットメッセージ 2 行目以降 (git の %b、改行含みうる。空文字許容)
 */
public record GitLogEntry(String shortHash, Instant commitTime, String author, String subject, String body) {}
