// ==================== ACCOUNTANT SPECIFIC JAVASCRIPT ====================

console.log('BlueMoon Portal - Accountant Module Loaded ✓');

// ==================== INVOICE MANAGEMENT ====================
function viewInvoiceDetails(invoiceId) {
    Alert.loading('Đang tải thông tin hóa đơn...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/accountant/invoices/${invoiceId}`;
    }, 500);
}

function deleteInvoice(invoiceId, invoiceNumber) {
    Alert.confirm(
        'Xác nhận xóa hóa đơn',
        `Bạn có chắc muốn xóa hóa đơn "${invoiceNumber}"? Hành động này không thể hoàn tác.`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Hóa đơn đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

function markInvoiceAsPaid(invoiceId, invoiceNumber) {
    Alert.confirm(
        'Xác nhận thanh toán',
        `Xác nhận hóa đơn "${invoiceNumber}" đã được thanh toán?`,
        () => {
            Alert.loading('Đang cập nhật...');
            
            setTimeout(() => {
                Alert.success('Đã cập nhật!', 'Hóa đơn đã được đánh dấu là đã thanh toán');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== FEE MANAGEMENT ====================
function viewFeeDetails(feeId) {
    Alert.loading('Đang tải thông tin khoản phí...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/accountant/fees/${feeId}`;
    }, 500);
}

function deleteFee(feeId, feeName) {
    Alert.confirm(
        'Xác nhận xóa khoản phí',
        `Bạn có chắc muốn xóa khoản phí "${feeName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Khoản phí đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== PAYMENT MANAGEMENT ====================
function recordPayment(apartmentId) {
    Swal.fire({
        title: 'Ghi nhận thanh toán',
        html: `
            <div style="text-align: left;">
                <div class="form-group">
                    <label class="form-label">Căn hộ:</label>
                    <input type="text" class="form-control" value="A${apartmentId}" readonly>
                </div>
                <div class="form-group">
                    <label class="form-label">Số tiền:</label>
                    <input type="number" id="paymentAmount" class="form-control" placeholder="Nhập số tiền">
                </div>
                <div class="form-group">
                    <label class="form-label">Phương thức:</label>
                    <select id="paymentMethod" class="form-control">
                        <option value="cash">Tiền mặt</option>
                        <option value="bank">Chuyển khoản</option>
                        <option value="card">Thẻ</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Ghi chú:</label>
                    <textarea id="paymentNote" class="form-control" rows="3"></textarea>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5',
        width: 500,
        preConfirm: () => {
            const amount = document.getElementById('paymentAmount').value;
            const method = document.getElementById('paymentMethod').value;
            const note = document.getElementById('paymentNote').value;
            
            if (!amount) {
                Swal.showValidationMessage('Vui lòng nhập số tiền');
                return false;
            }
            
            return { amount, method, note };
        }
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang ghi nhận thanh toán...');
            
            setTimeout(() => {
                Alert.success('Thành công!', 'Đã ghi nhận thanh toán');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    });
}

// ==================== FINANCIAL REPORTS ====================
function generateFinancialReport(reportType, period) {
    Alert.loading('Đang tạo báo cáo...');
    
    setTimeout(() => {
        Alert.success('Báo cáo đã sẵn sàng!', `Báo cáo ${reportType} ${period} đã được tạo`);
    }, 2000);
}

function exportFinancialData(format) {
    Alert.loading(`Đang xuất dữ liệu sang ${format.toUpperCase()}...`);
    
    setTimeout(() => {
        Alert.success('Xuất thành công!', `Dữ liệu tài chính đã được xuất ra file ${format.toUpperCase()}`);
    }, 1500);
}

// ==================== REVENUE STATISTICS ====================
function refreshRevenueStats() {
    Alert.loading('Đang cập nhật thống kê doanh thu...');
    
    setTimeout(() => {
        Alert.close();
        showNotification('Thống kê đã được cập nhật', 'success');
    }, 1000);
}

// ==================== ACCOUNTANT INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Accountant dashboard initialized');
    
    // Format all currency values
    const currencyElements = document.querySelectorAll('.currency');
    currencyElements.forEach(element => {
        const value = parseFloat(element.textContent);
        if (!isNaN(value)) {
            element.textContent = formatCurrency(value);
        }
    });
});

// ==================== INVOICE FORM VALIDATION ====================
function validateInvoiceForm() {
    const requiredFields = ['apartmentId', 'feeType', 'amount', 'dueDate'];
    let isValid = true;
    
    requiredFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field && !field.value.trim()) {
            isValid = false;
            field.classList.add('error');
        }
    });
    
    if (!isValid) {
        Alert.error('Lỗi', 'Vui lòng điền đầy đủ thông tin bắt buộc');
    }
    
    return isValid;
}
