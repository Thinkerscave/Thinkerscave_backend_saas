//package com.thinkerscave.common.orgm.filter;
//
//import com.thinkerscave.common.orgm.config.TenantContext;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
///**
// * TenantFilter extracts the 'X-Org-Code' header from each incoming HTTP request
// * and stores it in a thread-local context (TenantContext).
// *
// * This tenant identifier is later used by SchemaTenantResolver
// * to determine which database schema (tenant) Hibernate should use.
// *
// * This filter ensures multi-tenancy support via schema-based isolation.
// */
//
//@Component
//public class TenantFilter extends OncePerRequestFilter {
//
//    private static final String ORG_HEADER = "X-Org-Code";
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        // Extract organisation code from the request header
//        String orgCode = request.getHeader(ORG_HEADER);
//
//        // Set the tenant in the context if present, else default to 'public'
//        if (orgCode != null && !orgCode.isBlank()) {
//            TenantContext.setTenant(orgCode.trim().toLowerCase());
//        } else {
//            TenantContext.setTenant("public"); // fallback schema
//        }
//
//        try {
//            // Continue the filter chain
//            filterChain.doFilter(request, response);
//        } finally {
//            // Clear context to avoid tenant leaks in thread pool
//            TenantContext.clear();
//        }
//    }
//}
//
