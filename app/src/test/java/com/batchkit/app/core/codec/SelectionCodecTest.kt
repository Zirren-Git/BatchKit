package com.batchkit.app.core.codec

import com.batchkit.app.core.model.BatchAction
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SelectionCodecTest {

    @Test
    fun `package names round trip and are sorted and deduplicated`() {
        val encoded = SelectionCodec.encodePackages(
            listOf("com.b.example", "com.a.example", "com.b.example"),
        )
        assertThat(encoded).isEqualTo("com.a.example\ncom.b.example")
        assertThat(SelectionCodec.decodePackages(encoded))
            .containsExactly("com.a.example", "com.b.example")
            .inOrder()
    }

    @Test
    fun `invalid package names are rejected`() {
        assertThat(SelectionCodec.isValidPackageName("com.example.app")).isTrue()
        assertThat(SelectionCodec.isValidPackageName("com.example.app_1")).isTrue()
        assertThat(SelectionCodec.isValidPackageName("com")).isFalse()
        assertThat(SelectionCodec.isValidPackageName("")).isFalse()
        assertThat(SelectionCodec.isValidPackageName("com.example.app; rm -rf /")).isFalse()
        assertThat(SelectionCodec.isValidPackageName("com.example.app\nevil")).isFalse()
        assertThat(SelectionCodec.isValidPackageName("com.example.-bad")).isFalse()
    }

    @Test
    fun `decoding ignores trailing separators and whitespace`() {
        assertThat(SelectionCodec.decodePackages("\n com.a.example \n\ncom.b.example\n"))
            .containsExactly("com.a.example", "com.b.example")
            .inOrder()
        assertThat(SelectionCodec.decodePackages(null)).isEmpty()
        assertThat(SelectionCodec.decodePackages("")).isEmpty()
    }

    @Test
    fun `actions round trip through their stable ids`() {
        val actions = listOf(BatchAction.FORCE_STOP, BatchAction.BLOCK_BACKGROUND)
        val encoded = SelectionCodec.encodeActions(actions)
        assertThat(encoded).isEqualTo("force_stop,block_background")
        assertThat(SelectionCodec.decodeActions(encoded)).isEqualTo(actions)
    }

    @Test
    fun `unknown action ids are dropped instead of failing`() {
        assertThat(SelectionCodec.decodeActions("force_stop,not_a_real_action"))
            .containsExactly(BatchAction.FORCE_STOP)
    }
}
