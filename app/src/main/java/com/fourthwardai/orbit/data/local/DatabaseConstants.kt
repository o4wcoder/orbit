package com.fourthwardai.orbit.data.local

import com.fourthwardai.orbit.data.local.DatabaseConstants.Table.ARTICLES

class DatabaseConstants {

    object Feed {
        const val FEED_ID_MAIN = "main"
        const val FEED_ID_SAVED = "saved"
    }
    object Table {
        const val ARTICLES = "articles"
        const val CATEGORIES = "categories"
        const val ARTICLE_CATEGORY_CROSS_REF = "article_category_cross_ref"
    }

    object Query {

        const val SELECT_ALL_ARTICLES = "SELECT * FROM $ARTICLES"
        const val ORDER_BY_INGESTED_DESC = "ORDER BY ingestedAt DESC, id DESC"

        const val SELECT_ARTICLES_BY_CATEGORY = """
            SELECT 1
            FROM article_category_cross_ref acc
            WHERE acc.articleId = articles.id
            AND acc.categoryId
            """

        const val SELECT_ARTICLES_BY_CATEGORY_GROUP = """
            SELECT 1
            FROM article_category_cross_ref acc
            JOIN categories c ON c.id = acc.categoryId
            WHERE acc.articleId = articles.id
            AND c.`group`
            """
    }
}
