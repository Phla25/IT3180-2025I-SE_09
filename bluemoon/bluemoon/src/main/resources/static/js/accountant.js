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
        `Bạn có chắc muốn xóa hóa đơn "${invoiceNumber}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Hóa đơn đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
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
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== FEE MANAGEMENT ====================
function deleteFee(feeId, feeName) {
    Alert.confirm(
        'Xác nhận xóa khoản phí',
        `Bạn có chắc muốn xóa khoản phí "${feeName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Khoản phí đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
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
        Alert.success('Xuất thành công!', `Dữ liệu tài chính đã được xuất ra ${format.toUpperCase()}`);
    }, 1500);
}
