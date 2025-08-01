package io.github.techouse.qskotlin.fixtures.data

internal val EmptyTestCases: List<Map<String, Any>> =
    listOf(
        mapOf(
            "input" to "&",
            "withEmptyKeys" to emptyMap<String, Any?>(),
            "stringifyOutput" to mapOf("brackets" to "", "indices" to "", "repeat" to ""),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "&&",
            "withEmptyKeys" to emptyMap<String, Any?>(),
            "stringifyOutput" to mapOf("brackets" to "", "indices" to "", "repeat" to ""),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "&=",
            "withEmptyKeys" to mapOf("" to ""),
            "stringifyOutput" to mapOf("brackets" to "=", "indices" to "=", "repeat" to "="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "&=&",
            "withEmptyKeys" to mapOf("" to ""),
            "stringifyOutput" to mapOf("brackets" to "=", "indices" to "=", "repeat" to "="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "&=&=",
            "withEmptyKeys" to mapOf("" to listOf("", "")),
            "stringifyOutput" to
                mapOf("brackets" to "[]=&[]=", "indices" to "[0]=&[1]=", "repeat" to "=&="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "&=&=&",
            "withEmptyKeys" to mapOf("" to listOf("", "")),
            "stringifyOutput" to
                mapOf("brackets" to "[]=&[]=", "indices" to "[0]=&[1]=", "repeat" to "=&="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "=",
            "withEmptyKeys" to mapOf("" to ""),
            "noEmptyKeys" to emptyMap<String, Any?>(),
            "stringifyOutput" to mapOf("brackets" to "=", "indices" to "=", "repeat" to "="),
        ),
        mapOf(
            "input" to "=&",
            "withEmptyKeys" to mapOf("" to ""),
            "stringifyOutput" to mapOf("brackets" to "=", "indices" to "=", "repeat" to "="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "=&&&",
            "withEmptyKeys" to mapOf("" to ""),
            "stringifyOutput" to mapOf("brackets" to "=", "indices" to "=", "repeat" to "="),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "=&=&=&",
            "withEmptyKeys" to mapOf("" to listOf("", "", "")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "[]=&[]=&[]=",
                    "indices" to "[0]=&[1]=&[2]=",
                    "repeat" to "=&=&=",
                ),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "=&a[]=b&a[1]=c",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c",
                    "indices" to "=&a[0]=b&a[1]=c",
                    "repeat" to "=&a=b&a=c",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c")),
        ),
        mapOf(
            "input" to "=a",
            "withEmptyKeys" to mapOf("" to "a"),
            "noEmptyKeys" to emptyMap<String, Any?>(),
            "stringifyOutput" to mapOf("brackets" to "=a", "indices" to "=a", "repeat" to "=a"),
        ),
        mapOf(
            "input" to "a==a",
            "withEmptyKeys" to mapOf("a" to "=a"),
            "noEmptyKeys" to mapOf("a" to "=a"),
            "stringifyOutput" to
                mapOf("brackets" to "a==a", "indices" to "a==a", "repeat" to "a==a"),
        ),
        mapOf(
            "input" to "=&a[]=b",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b")),
            "stringifyOutput" to
                mapOf("brackets" to "=&a[]=b", "indices" to "=&a[0]=b", "repeat" to "=&a=b"),
            "noEmptyKeys" to mapOf("a" to listOf("b")),
        ),
        mapOf(
            "input" to "=&a[]=b&a[]=c&a[2]=d",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c", "d")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c&a[]=d",
                    "indices" to "=&a[0]=b&a[1]=c&a[2]=d",
                    "repeat" to "=&a=b&a=c&a=d",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c", "d")),
        ),
        mapOf(
            "input" to "=a&=b",
            "withEmptyKeys" to mapOf("" to listOf("a", "b")),
            "stringifyOutput" to
                mapOf("brackets" to "[]=a&[]=b", "indices" to "[0]=a&[1]=b", "repeat" to "=a&=b"),
            "noEmptyKeys" to emptyMap<String, Any?>(),
        ),
        mapOf(
            "input" to "=a&foo=b",
            "withEmptyKeys" to mapOf("" to "a", "foo" to "b"),
            "noEmptyKeys" to mapOf("foo" to "b"),
            "stringifyOutput" to
                mapOf("brackets" to "=a&foo=b", "indices" to "=a&foo=b", "repeat" to "=a&foo=b"),
        ),
        mapOf(
            "input" to "a[]=b&a=c&=",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c",
                    "indices" to "=&a[0]=b&a[1]=c",
                    "repeat" to "=&a=b&a=c",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c")),
        ),
        mapOf(
            "input" to "a[0]=b&a=c&=",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c",
                    "indices" to "=&a[0]=b&a[1]=c",
                    "repeat" to "=&a=b&a=c",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c")),
        ),
        mapOf(
            "input" to "a=b&a[]=c&=",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c",
                    "indices" to "=&a[0]=b&a[1]=c",
                    "repeat" to "=&a=b&a=c",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c")),
        ),
        mapOf(
            "input" to "a=b&a[0]=c&=",
            "withEmptyKeys" to mapOf("" to "", "a" to listOf("b", "c")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "=&a[]=b&a[]=c",
                    "indices" to "=&a[0]=b&a[1]=c",
                    "repeat" to "=&a=b&a=c",
                ),
            "noEmptyKeys" to mapOf("a" to listOf("b", "c")),
        ),
        mapOf(
            "input" to "[]=a&[]=b& []=1",
            "withEmptyKeys" to mapOf("" to listOf("a", "b"), " " to listOf("1")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "[]=a&[]=b& []=1",
                    "indices" to "[0]=a&[1]=b& [0]=1",
                    "repeat" to "=a&=b& =1",
                ),
            "noEmptyKeys" to mapOf(0 to "a", 1 to "b", " " to listOf("1")),
        ),
        mapOf(
            "input" to "[0]=a&[1]=b&a[0]=1&a[1]=2",
            "withEmptyKeys" to mapOf("" to listOf("a", "b"), "a" to listOf("1", "2")),
            "noEmptyKeys" to mapOf(0 to "a", 1 to "b", "a" to listOf("1", "2")),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "[]=a&[]=b&a[]=1&a[]=2",
                    "indices" to "[0]=a&[1]=b&a[0]=1&a[1]=2",
                    "repeat" to "=a&=b&a=1&a=2",
                ),
        ),
        mapOf(
            "input" to "[deep]=a&[deep]=2",
            "withEmptyKeys" to mapOf("" to mapOf("deep" to listOf("a", "2"))),
            "stringifyOutput" to
                mapOf(
                    "brackets" to "[deep][]=a&[deep][]=2",
                    "indices" to "[deep][0]=a&[deep][1]=2",
                    "repeat" to "[deep]=a&[deep]=2",
                ),
            "noEmptyKeys" to mapOf("deep" to listOf("a", "2")),
        ),
        mapOf(
            "input" to "%5B0%5D=a&%5B1%5D=b",
            "withEmptyKeys" to mapOf("" to listOf("a", "b")),
            "stringifyOutput" to
                mapOf("brackets" to "[]=a&[]=b", "indices" to "[0]=a&[1]=b", "repeat" to "=a&=b"),
            "noEmptyKeys" to mapOf(0 to "a", 1 to "b"),
        ),
    )
