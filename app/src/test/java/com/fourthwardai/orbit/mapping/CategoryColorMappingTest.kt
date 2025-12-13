package com.fourthwardai.orbit.mapping

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fourthwardai.orbit.data.local.CategoryEntity
import com.fourthwardai.orbit.data.local.toDomain
import com.fourthwardai.orbit.data.local.toEntity
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.toDomain
import com.fourthwardai.orbit.network.dto.CategoryDto
import org.junit.Test

class CategoryColorMappingTest {

    @Test
    fun categoryDtoDomainEntityRoundtripPreservesColors() {
        val dto = CategoryDto(
            id = "c1",
            name = "Cat",
            group = "grp",
            colorLight = "#FF112233",
            colorDark = "#FF445566",
        )

        // DTO -> domain
        val domain: Category = dto.toDomain()
        // domain -> entity
        val entity: CategoryEntity = domain.toEntity()
        // entity -> domain
        val roundTripped: Category = entity.toDomain()

        // Ensure colors preserved (exact equality of ARGB ints)
        assertThat(roundTripped.colorLight.value).isEqualTo(domain.colorLight.value)
        assertThat(roundTripped.colorDark.value).isEqualTo(domain.colorDark.value)
    }
}
