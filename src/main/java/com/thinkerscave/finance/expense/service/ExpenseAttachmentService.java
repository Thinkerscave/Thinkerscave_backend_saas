package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
public interface ExpenseAttachmentService {
 AttachmentResponse upload(Long expenseId,MultipartFile file,String kind); List<AttachmentResponse> list(Long expenseId);
 Download download(Long expenseId,Long documentId); void delete(Long expenseId,Long documentId);
}
