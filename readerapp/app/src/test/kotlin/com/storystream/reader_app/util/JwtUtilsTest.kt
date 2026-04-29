package com.storystream.reader_app.util

import org.junit.Assert.*
import org.junit.Test

class JwtUtilsTest {

    @Test
    fun `test isExpired with expired token returns true`() {
        val expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.4Adcj3UFYzPUVaVF43FmMze2BqJ3P9vF8rY0GzW0bXM" // exp: 1516239022 (past)
        assertTrue(JwtUtils.isExpired(expiredToken))
    }

    @Test
    fun `test isExpired with valid token returns false`() {
        val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE3NzI1MjE1NTF9.4Adcj3UFYzPUVaVF43FmMze2BqJ3P9vF8rY0GzW0bXM" // exp: 1772521551 (future)
        assertFalse(JwtUtils.isExpired(validToken))
    }

    @Test
    fun `test isExpired with invalid token returns true`() {
        val invalidToken = "invalid.jwt.token"
        assertTrue(JwtUtils.isExpired(invalidToken))
    }

    @Test
    fun `test decodeClaims with valid token returns claims`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0VXNlciIsInRpZXIiOiJGUkVFIn0.signature" // mock token
        val claims = JwtUtils.decodeClaims(token)
        assertNotNull(claims)
        assertEquals("testUser", claims?.getString("sub"))
        assertEquals("FREE", claims?.getString("tier"))
    }

    @Test
    fun `test decodeClaims with invalid token returns null`() {
        val invalidToken = "invalid.jwt.token"
        val claims = JwtUtils.decodeClaims(invalidToken)
        assertNull(claims)
    }
}
