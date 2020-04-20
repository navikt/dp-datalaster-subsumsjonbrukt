package no.nav.dp.datalaster.subsumsjonbrukt.regelapi

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID
import org.junit.Test

class SubsumsjonIdTest {

    @Test
    fun `Subsumsjon id should be in ULID format`() {
        val id = ULID().nextULID()
        val inntektId = SubsumsjonId(id)

        id shouldBe inntektId.id
    }

    @Test
    fun `Subsumsjon id not in ULID format should fail`() {
        val id = UUID.randomUUID().toString()
        shouldThrow<IllegalArgumentException> { SubsumsjonId(id) }
    }

    @Test
    fun `Should be able to create Subsumsjon id from defined json`() {
        val id = SubsumsjonId.fromJson(""" {"id" : "${ULID().nextULID()}"}""")
        id shouldNotBe null
    }

    @Test
    fun `Should not be able to create Subsumsjon id from json`() {
        val id = SubsumsjonId.fromJson(""" {"id" : "${UUID.randomUUID()}"}""")
        id shouldBe null
    }

    @Test
    fun `Should not be able to create Subsumsjon id from non valid json`() {
        val id = SubsumsjonId.fromJson("bla")
        id shouldBe null
    }
}
