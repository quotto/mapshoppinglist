package com.mapshoppinglist.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mapshoppinglist.R

private data class PolicySection(val title: String, val lines: List<String>, val contactEmail: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyRoute(onBack: () -> Unit) {
    val updatedLabel = stringResource(R.string.privacy_policy_updated, "2025年10月4日")
    val sections = listOf(
        PolicySection(
            title = stringResource(R.string.privacy_policy_section1_title),
            lines = stringResource(R.string.privacy_policy_section1_body).split("\n")
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section2_title),
            lines = stringResource(R.string.privacy_policy_section2_body).split("\n")
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section3_title),
            lines = stringResource(R.string.privacy_policy_section3_body).split("\n")
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section4_title),
            lines = stringResource(R.string.privacy_policy_section4_body).split("\n")
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section5_title),
            lines = stringResource(R.string.privacy_policy_section5_body).split("\n")
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section6_title),
            lines = stringResource(R.string.privacy_policy_section6_body).split("\n"),
            contactEmail = stringResource(R.string.privacy_policy_contact_email)
        ),
        PolicySection(
            title = stringResource(R.string.privacy_policy_section7_title),
            lines = stringResource(R.string.privacy_policy_section7_body).split("\n")
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privacy_policy_title),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = updatedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    text = stringResource(R.string.privacy_policy_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(sections) { section ->
                val uriHandler = LocalUriHandler.current
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                section.lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                section.contactEmail?.let { email ->
                    val annotated = buildAnnotatedString {
                        val tag = "email"
                        pushStringAnnotation(tag, "mailto:$email")
                        withStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ) {
                            append(email)
                        }
                        pop()
                    }
                    Text(
                        text = annotated,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                annotated.getStringAnnotations("email", 0, annotated.length)
                                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
                            },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
