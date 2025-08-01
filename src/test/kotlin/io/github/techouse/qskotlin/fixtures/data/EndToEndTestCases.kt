package io.github.techouse.qskotlin.fixtures.data

internal data class EndToEndTestCase(val data: Any?, val encoded: String)

internal val EndToEndTestCases: List<EndToEndTestCase> =
    listOf(
        // empty dict
        EndToEndTestCase(data = emptyMap<String, Any?>(), encoded = ""),

        // simple dict with single key-value pair
        EndToEndTestCase(data = mapOf("a" to "b"), encoded = "a=b"),

        // simple dict with multiple key-value pairs 1
        EndToEndTestCase(data = mapOf("a" to "b", "c" to "d"), encoded = "a=b&c=d"),

        // simple dict with multiple key-value pairs 2
        EndToEndTestCase(data = mapOf("a" to "b", "c" to "d", "e" to "f"), encoded = "a=b&c=d&e=f"),

        // dict with list
        EndToEndTestCase(
            data = mapOf("a" to "b", "c" to "d", "e" to listOf("f", "g", "h")),
            encoded = "a=b&c=d&e[0]=f&e[1]=g&e[2]=h",
        ),

        // dict with list and nested dict
        EndToEndTestCase(
            data =
                mapOf(
                    "a" to "b",
                    "c" to "d",
                    "e" to listOf("f", "g", "h"),
                    "i" to mapOf("j" to "k", "l" to "m"),
                ),
            encoded = "a=b&c=d&e[0]=f&e[1]=g&e[2]=h&i[j]=k&i[l]=m",
        ),

        // simple 1-level nested dict
        EndToEndTestCase(data = mapOf("a" to mapOf("b" to "c")), encoded = "a[b]=c"),

        // two-level nesting
        EndToEndTestCase(
            data = mapOf("a" to mapOf("b" to mapOf("c" to "d"))),
            encoded = "a[b][c]=d",
        ),

        // list of dicts
        EndToEndTestCase(
            data = mapOf("a" to listOf(mapOf("b" to "c"), mapOf("d" to "e"))),
            encoded = "a[0][b]=c&a[1][d]=e",
        ),

        // single-item list
        EndToEndTestCase(data = mapOf("a" to listOf("f")), encoded = "a[0]=f"),

        // nested list inside a dict inside a list
        EndToEndTestCase(
            data = mapOf("a" to listOf(mapOf("b" to listOf("c")))),
            encoded = "a[0][b][0]=c",
        ),

        // empty-string value
        EndToEndTestCase(data = mapOf("a" to ""), encoded = "a="),

        // list containing an empty string
        EndToEndTestCase(data = mapOf("a" to listOf("", "b")), encoded = "a[0]=&a[1]=b"),

        // unicode-only key and value
        EndToEndTestCase(data = mapOf("キー" to "値"), encoded = "キー=値"),

        // emoji (multi-byte unicode) in key and value
        EndToEndTestCase(data = mapOf("🙂" to "😊"), encoded = "🙂=😊"),

        // complex dict with special characters
        EndToEndTestCase(
            data =
                mapOf(
                    "filters" to
                        mapOf(
                            "\$or" to
                                listOf(
                                    mapOf("date" to mapOf("\$eq" to "2020-01-01")),
                                    mapOf("date" to mapOf("\$eq" to "2020-01-02")),
                                ),
                            "author" to mapOf("name" to mapOf("\$eq" to "John Doe")),
                        )
                ),
            encoded =
                "filters[\$or][0][date][\$eq]=2020-01-01&filters[\$or][1][date][\$eq]=2020-01-02&filters[author][name][\$eq]=John Doe",
        ),

        // dart_api_query/comments_embed_response
        EndToEndTestCase(
            data =
                mapOf(
                    "commentsEmbedResponse" to
                        listOf(
                            mapOf(
                                "id" to "1",
                                "post_id" to "1",
                                "someId" to "ma018-9ha12",
                                "text" to "Hello",
                                "replies" to
                                    listOf(
                                        mapOf(
                                            "id" to "3",
                                            "comment_id" to "1",
                                            "someId" to "ma020-9ha15",
                                            "text" to "Hello",
                                        )
                                    ),
                            ),
                            mapOf(
                                "id" to "2",
                                "post_id" to "1",
                                "someId" to "mw012-7ha19",
                                "text" to "How are you?",
                                "replies" to
                                    listOf(
                                        mapOf(
                                            "id" to "4",
                                            "comment_id" to "2",
                                            "someId" to "mw023-9ha18",
                                            "text" to "Hello",
                                        ),
                                        mapOf(
                                            "id" to "5",
                                            "comment_id" to "2",
                                            "someId" to "mw035-0ha22",
                                            "text" to "Hello",
                                        ),
                                    ),
                            ),
                        )
                ),
            encoded =
                "commentsEmbedResponse[0][id]=1&commentsEmbedResponse[0][post_id]=1&commentsEmbedResponse[0][someId]=ma018-9ha12&commentsEmbedResponse[0][text]=Hello&commentsEmbedResponse[0][replies][0][id]=3&commentsEmbedResponse[0][replies][0][comment_id]=1&commentsEmbedResponse[0][replies][0][someId]=ma020-9ha15&commentsEmbedResponse[0][replies][0][text]=Hello&commentsEmbedResponse[1][id]=2&commentsEmbedResponse[1][post_id]=1&commentsEmbedResponse[1][someId]=mw012-7ha19&commentsEmbedResponse[1][text]=How are you?&commentsEmbedResponse[1][replies][0][id]=4&commentsEmbedResponse[1][replies][0][comment_id]=2&commentsEmbedResponse[1][replies][0][someId]=mw023-9ha18&commentsEmbedResponse[1][replies][0][text]=Hello&commentsEmbedResponse[1][replies][1][id]=5&commentsEmbedResponse[1][replies][1][comment_id]=2&commentsEmbedResponse[1][replies][1][someId]=mw035-0ha22&commentsEmbedResponse[1][replies][1][text]=Hello",
        ),

        // dart_api_query/comments_response
        EndToEndTestCase(
            data =
                mapOf(
                    "commentsResponse" to
                        listOf(
                            mapOf(
                                "id" to "1",
                                "post_id" to "1",
                                "someId" to "ma018-9ha12",
                                "text" to "Hello",
                                "replies" to
                                    listOf(
                                        mapOf(
                                            "id" to "3",
                                            "comment_id" to "1",
                                            "someId" to "ma020-9ha15",
                                            "text" to "Hello",
                                        )
                                    ),
                            ),
                            mapOf(
                                "id" to "2",
                                "post_id" to "1",
                                "someId" to "mw012-7ha19",
                                "text" to "How are you?",
                                "replies" to
                                    listOf(
                                        mapOf(
                                            "id" to "4",
                                            "comment_id" to "2",
                                            "someId" to "mw023-9ha18",
                                            "text" to "Hello",
                                        ),
                                        mapOf(
                                            "id" to "5",
                                            "comment_id" to "2",
                                            "someId" to "mw035-0ha22",
                                            "text" to "Hello",
                                        ),
                                    ),
                            ),
                        )
                ),
            encoded =
                "commentsResponse[0][id]=1&commentsResponse[0][post_id]=1&commentsResponse[0][someId]=ma018-9ha12&commentsResponse[0][text]=Hello&commentsResponse[0][replies][0][id]=3&commentsResponse[0][replies][0][comment_id]=1&commentsResponse[0][replies][0][someId]=ma020-9ha15&commentsResponse[0][replies][0][text]=Hello&commentsResponse[1][id]=2&commentsResponse[1][post_id]=1&commentsResponse[1][someId]=mw012-7ha19&commentsResponse[1][text]=How are you?&commentsResponse[1][replies][0][id]=4&commentsResponse[1][replies][0][comment_id]=2&commentsResponse[1][replies][0][someId]=mw023-9ha18&commentsResponse[1][replies][0][text]=Hello&commentsResponse[1][replies][1][id]=5&commentsResponse[1][replies][1][comment_id]=2&commentsResponse[1][replies][1][someId]=mw035-0ha22&commentsResponse[1][replies][1][text]=Hello",
        ),

        // dart_api_query/post_embed_response
        EndToEndTestCase(
            data =
                mapOf(
                    "data" to
                        mapOf(
                            "id" to "1",
                            "someId" to "af621-4aa41",
                            "text" to "Lorem Ipsum Dolor",
                            "user" to
                                mapOf("firstname" to "John", "lastname" to "Doe", "age" to "25"),
                            "relationships" to
                                mapOf(
                                    "tags" to
                                        mapOf(
                                            "data" to
                                                listOf(
                                                    mapOf("name" to "super"),
                                                    mapOf("name" to "awesome"),
                                                )
                                        )
                                ),
                        )
                ),
            encoded =
                "data[id]=1&data[someId]=af621-4aa41&data[text]=Lorem Ipsum Dolor&data[user][firstname]=John&data[user][lastname]=Doe&data[user][age]=25&data[relationships][tags][data][0][name]=super&data[relationships][tags][data][1][name]=awesome",
        ),

        // dart_api_query/post_response
        EndToEndTestCase(
            data =
                mapOf(
                    "id" to "1",
                    "someId" to "af621-4aa41",
                    "text" to "Lorem Ipsum Dolor",
                    "user" to mapOf("firstname" to "John", "lastname" to "Doe", "age" to "25"),
                    "relationships" to
                        mapOf(
                            "tags" to listOf(mapOf("name" to "super"), mapOf("name" to "awesome"))
                        ),
                ),
            encoded =
                "id=1&someId=af621-4aa41&text=Lorem Ipsum Dolor&user[firstname]=John&user[lastname]=Doe&user[age]=25&relationships[tags][0][name]=super&relationships[tags][1][name]=awesome",
        ),

        // dart_api_query/posts_response
        EndToEndTestCase(
            data =
                mapOf(
                    "postsResponse" to
                        listOf(
                            mapOf(
                                "id" to "1",
                                "someId" to "du761-8bc98",
                                "text" to "Lorem Ipsum Dolor",
                                "user" to
                                    mapOf(
                                        "firstname" to "John",
                                        "lastname" to "Doe",
                                        "age" to "25",
                                    ),
                                "relationships" to
                                    mapOf(
                                        "tags" to
                                            listOf(
                                                mapOf("name" to "super"),
                                                mapOf("name" to "awesome"),
                                            )
                                    ),
                            ),
                            mapOf(
                                "id" to "1",
                                "someId" to "pa813-7jx02",
                                "text" to "Lorem Ipsum Dolor",
                                "user" to
                                    mapOf(
                                        "firstname" to "Mary",
                                        "lastname" to "Doe",
                                        "age" to "25",
                                    ),
                                "relationships" to
                                    mapOf(
                                        "tags" to
                                            listOf(
                                                mapOf("name" to "super"),
                                                mapOf("name" to "awesome"),
                                            )
                                    ),
                            ),
                        )
                ),
            encoded =
                "postsResponse[0][id]=1&postsResponse[0][someId]=du761-8bc98&postsResponse[0][text]=Lorem Ipsum Dolor&postsResponse[0][user][firstname]=John&postsResponse[0][user][lastname]=Doe&postsResponse[0][user][age]=25&postsResponse[0][relationships][tags][0][name]=super&postsResponse[0][relationships][tags][1][name]=awesome&postsResponse[1][id]=1&postsResponse[1][someId]=pa813-7jx02&postsResponse[1][text]=Lorem Ipsum Dolor&postsResponse[1][user][firstname]=Mary&postsResponse[1][user][lastname]=Doe&postsResponse[1][user][age]=25&postsResponse[1][relationships][tags][0][name]=super&postsResponse[1][relationships][tags][1][name]=awesome",
        ),

        // dart_api_query/posts_response_paginate
        EndToEndTestCase(
            data =
                mapOf(
                    "posts" to
                        listOf(
                            mapOf(
                                "id" to "1",
                                "someId" to "du761-8bc98",
                                "text" to "Lorem Ipsum Dolor",
                                "user" to
                                    mapOf(
                                        "firstname" to "John",
                                        "lastname" to "Doe",
                                        "age" to "25",
                                    ),
                                "relationships" to
                                    mapOf(
                                        "tags" to
                                            listOf(
                                                mapOf("name" to "super"),
                                                mapOf("name" to "awesome"),
                                            )
                                    ),
                            ),
                            mapOf(
                                "id" to "1",
                                "someId" to "pa813-7jx02",
                                "text" to "Lorem Ipsum Dolor",
                                "user" to
                                    mapOf(
                                        "firstname" to "Mary",
                                        "lastname" to "Doe",
                                        "age" to "25",
                                    ),
                                "relationships" to
                                    mapOf(
                                        "tags" to
                                            listOf(
                                                mapOf("name" to "super"),
                                                mapOf("name" to "awesome"),
                                            )
                                    ),
                            ),
                        ),
                    "total" to "2",
                ),
            encoded =
                "posts[0][id]=1&posts[0][someId]=du761-8bc98&posts[0][text]=Lorem Ipsum Dolor&posts[0][user][firstname]=John&posts[0][user][lastname]=Doe&posts[0][user][age]=25&posts[0][relationships][tags][0][name]=super&posts[0][relationships][tags][1][name]=awesome&posts[1][id]=1&posts[1][someId]=pa813-7jx02&posts[1][text]=Lorem Ipsum Dolor&posts[1][user][firstname]=Mary&posts[1][user][lastname]=Doe&posts[1][user][age]=25&posts[1][relationships][tags][0][name]=super&posts[1][relationships][tags][1][name]=awesome&total=2",
        ),
    )
